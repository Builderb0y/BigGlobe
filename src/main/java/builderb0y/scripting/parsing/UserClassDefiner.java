package builderb0y.scripting.parsing;

import java.lang.invoke.StringConcatFactory;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.HashCommon;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class UserClassDefiner {

	public static final MethodInfo
		OBJECT_CONSTRUCTOR         = MethodInfo.getConstructor(Object.class),
		MAKE_CONCAT_WITH_CONSTANTS = MethodInfo.getMethod(StringConcatFactory.class, "makeConcatWithConstants"),
		HASH_MIX                   = MethodInfo.findMethod(HashCommon.class, "mix", int.class, int.class).pure();


	public final ExpressionParser parser;
	public final String className;
	public final ClassCompileContext innerClass;
	public final TypeInfo innerClassType;

	public UserClassDefiner(ExpressionParser parser, String className) {
		this.parser = parser;
		this.className = className;
		this.innerClass = parser.clazz.newInnerClass(
			ACC_PUBLIC | ACC_STATIC,
			parser.clazz.innerClassName(className),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.innerClassType = this.innerClass.info;
	}

	public List<FieldCompileContext> parse() throws ScriptParsingException {
		List<FieldCompileContext> fields = this.parseFields();
		List<FieldCompileContext> nonDefaulted = fields.stream().filter(field -> field.initializer == null).toList();
		this.addConstructors(fields, nonDefaulted);
		this.addToString(fields);
		this.addEquals(fields);
		this.addHashCode(fields);
		this.exposeToScript(fields, nonDefaulted);
		return fields;
	}

	public List<FieldCompileContext> parseFields() throws ScriptParsingException {
		this.parser.input.expectAfterWhitespace('(');
		List<FieldCompileContext> fields = new ArrayList<>(8);
		while (!this.parser.input.hasAfterWhitespace(')')) {
			String typeName = this.parser.input.expectIdentifier();
			TypeInfo type = this.parser.environment.getType(this.parser, typeName);
			if (type == null) throw new ScriptParsingException("Unknown type: " + typeName, this.parser.input);

			String fieldName = this.parser.verifyName(this.parser.input.expectIdentifierAfterWhitespace(), "field");
			FieldCompileContext field = this.innerClass.newField(ACC_PUBLIC, fieldName, type);
			fields.add(field);

			if (this.parser.input.hasOperatorAfterWhitespace("=")) {
				ConstantValue initializer = this.parser.nextSingleExpression().cast(this.parser, type, CastMode.IMPLICIT_THROW).getConstantValue();
				if (initializer.isConstant()) {
					field.initializer = initializer;
				}
				else {
					throw new ScriptParsingException("Field initializer must be constant", this.parser.input);
				}
			}
			FieldInfo fieldInfo = field.info;
			this.innerClass.newMethod(ACC_PUBLIC, fieldName, type).scopes.withScope((MethodCompileContext getter) -> {
				return_(getField(load("parser", 0, this.innerClassType), fieldInfo)).emitBytecode(getter);
			});
			this.innerClass.newMethod(ACC_PUBLIC, fieldName, TypeInfos.VOID, fieldInfo.type).scopes.withScope((MethodCompileContext setter) -> {
				putField(load("parser", 0, this.innerClassType), fieldInfo, load(fieldInfo.name, 1, fieldInfo.type)).emitBytecode(setter);
				return_(noop).emitBytecode(setter);
			});

			this.parser.input.hasOperatorAfterWhitespace(",,");
		}
		return fields;
	}

	public void addConstructors(List<FieldCompileContext> fields, List<FieldCompileContext> nonDefaulted) {
		this.innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID).scopes.withScope((MethodCompileContext constructor) -> {
			VarInfo constructorParser = constructor.addThis();
			invokeInstance(load(constructorParser), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
			for (FieldCompileContext field : fields) {
				if (field.initializer != null) {
					putField(
						load(constructorParser),
						new FieldInfo(ACC_PUBLIC, this.innerClassType, field.name(), field.info.type),
						ldc(field.initializer)
					)
					.emitBytecode(constructor);
				}
			}
			return_(noop).emitBytecode(constructor);
		});
		if (!fields.isEmpty()) {
			this.innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)).scopes.withScope((MethodCompileContext constructor) -> {
				VarInfo constructorParser = constructor.addThis();
				invokeInstance(load(constructorParser), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorParser),
						new FieldInfo(ACC_PUBLIC, this.innerClassType, field.name(), field.info.type),
						load(constructor.newParameter(field.name(), field.info.type))
					)
					.emitBytecode(constructor);
				}
				return_(noop).emitBytecode(constructor);
			});
		}
		if (nonDefaulted.size() != fields.size() && !nonDefaulted.isEmpty()) {
			this.innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID, nonDefaulted.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)).scopes.withScope((MethodCompileContext constructor) -> {
				VarInfo constructorParser = constructor.addThis();
				invokeInstance(load(constructorParser), OBJECT_CONSTRUCTOR).emitBytecode(constructor);
				for (FieldCompileContext field : fields) {
					putField(
						load(constructorParser),
						new FieldInfo(ACC_PUBLIC, this.innerClassType, field.info.name, field.info.type),
						field.initializer != null ? ldc(field.initializer) : load(constructor.newParameter(field.name(), field.info.type))
					)
					.emitBytecode(constructor);
				}
				return_(noop).emitBytecode(constructor);
			});
		}
	}

	public void addToString(List<FieldCompileContext> fields) {
		this.innerClass.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodParser = method.addThis();
			StringBuilder pattern = new StringBuilder(this.className).append('(');
			for (FieldCompileContext field : fields) {
				pattern.append(field.name()).append(": ").append('\u0001').append(", ");
			}
			pattern.setLength(pattern.length() - 2);
			pattern.append(')');
			return_(
				invokeDynamic(
					MAKE_CONCAT_WITH_CONSTANTS,
					new MethodInfo(
						ACC_PUBLIC | ACC_STATIC,
						TypeInfos.OBJECT,
						"toString",
						TypeInfos.STRING,
						fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)
					),
					new ConstantValue[] {
						constant(pattern.toString())
					},
					fields
					.stream()
					.map(field -> getField(load(methodParser), field.info))
					.toArray(InsnTree.ARRAY_FACTORY)
				)
			)
			.emitBytecode(method);
		});
	}

	public void addHashCode(List<FieldCompileContext> fields) {
		this.innerClass.newMethod(ACC_PUBLIC, "hashCode", TypeInfos.INT).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodParser = method.addThis();
			if (fields.isEmpty()) {
				return_(ldc(0)).emitBytecode(method);
			}
			else {
				invokeStatic(
					HASH_MIX,
					ArrayExtensions.computeHashCode(
						getField(load(methodParser), fields.get(0).info)
					)
				)
				.emitBytecode(method);
				for (int index = 1, size = fields.size(); index < size; index++) {
					invokeStatic(
						HASH_MIX,
						add(
							this.parser,
							getFromStack(TypeInfos.INT),
							ArrayExtensions.computeHashCode(
								getField(load(methodParser), fields.get(index).info)
							)
						)
					)
					.emitBytecode(method);
				}
				return_(getFromStack(TypeInfos.INT)).emitBytecode(method);
			}
		});
	}

	public void addEquals(List<FieldCompileContext> fields) {
		this.innerClass.newMethod(ACC_PUBLIC, "equals", TypeInfos.BOOLEAN, TypeInfos.OBJECT).scopes.withScope((MethodCompileContext method) -> {
			VarInfo methodParser = method.addThis();
			VarInfo object = method.newParameter("object", TypeInfos.OBJECT);
			if (fields.isEmpty()) {
				return_(instanceOf(load(object), this.innerClassType)).emitBytecode(method);
			}
			else {
				VarInfo that = method.newVariable("that", this.innerClassType);
				ifThen(
					not(condition(this.parser, instanceOf(load(object), this.innerClassType))),
					return_(ldc(false))
				)
				.emitBytecode(method);
				store(that, load(object).cast(this.parser, this.innerClassType, CastMode.EXPLICIT_THROW)).emitBytecode(method);
				for (FieldCompileContext field : fields) {
					ifThen(
						not(
							condition(
								this.parser,
								ArrayExtensions.computeEquals(
									this.parser,
									getField(load(methodParser), field.info),
									getField(load(that), field.info)
								)
							)
						),
						return_(ldc(false))
					)
					.emitBytecode(method);
				}
				return_(ldc(true)).emitBytecode(method);
			}
		});
	}

	public void exposeToScript(List<FieldCompileContext> fields, List<FieldCompileContext> nonDefaulted) {
		this.parser.environment.user().types.put(this.className, this.innerClassType);
		this.parser.environment.user().addConstructor(this.innerClassType, new MethodInfo(ACC_PUBLIC, this.innerClassType, "<init>", TypeInfos.VOID));
		if (!fields.isEmpty()) {
			this.parser.environment.user().addConstructor(this.innerClassType, new MethodInfo(ACC_PUBLIC, this.innerClassType, "<init>", TypeInfos.VOID, fields.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			if (nonDefaulted.size() != fields.size()) {
				this.parser.environment.user().addConstructor(this.innerClassType, new MethodInfo(ACC_PUBLIC, this.innerClassType, "<init>", TypeInfos.VOID, nonDefaulted.stream().map(field -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)));
			}
		}
		fields.stream().map(field -> field.info).forEach(this.parser.environment.user()::addFieldGetterAndSetter);
	}
}