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
		List<FieldCompileContext> nonDefaulted = fields.stream().filter((FieldCompileContext field) -> field.initializer == null).toList();
		this.addConstructors(fields, nonDefaulted);
		addToString(this.innerClass, this.className, fields);
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
			{
				MethodCompileContext getter = this.innerClass.newMethod(ACC_PUBLIC, fieldName, type);
				LazyVarInfo self = new LazyVarInfo("this", getter.clazz.info);
				return_(getField(load(self), fieldInfo)).emitBytecode(getter);
				getter.endCode();
			}
			{
				MethodCompileContext setter = this.innerClass.newMethod(ACC_PUBLIC, fieldName, TypeInfos.VOID, new LazyVarInfo(fieldName, fieldInfo.type));
				LazyVarInfo self = new LazyVarInfo("this", setter.clazz.info);
				LazyVarInfo value = new LazyVarInfo(fieldInfo.name, fieldInfo.type);
				putField(load(self), fieldInfo, load(value)).emitBytecode(setter);
				return_(noop).emitBytecode(setter);
				setter.endCode();
			}

			this.parser.input.hasOperatorAfterWhitespace(",,");
		}
		return fields;
	}

	public void addConstructors(List<FieldCompileContext> fields, List<FieldCompileContext> nonDefaulted) {
		{
			MethodCompileContext noArgConstructor = this.innerClass.newMethod(ACC_PUBLIC, "<init>", TypeInfos.VOID);
			LazyVarInfo self = new LazyVarInfo("this", noArgConstructor.clazz.info);
			invokeInstance(load(self), OBJECT_CONSTRUCTOR).emitBytecode(noArgConstructor);
			for (FieldCompileContext field : fields) {
				if (field.initializer != null) {
					putField(
						load(self),
						new FieldInfo(ACC_PUBLIC, this.innerClassType, field.name(), field.info.type),
						ldc(field.initializer)
					)
					.emitBytecode(noArgConstructor);
				}
			}
			return_(noop).emitBytecode(noArgConstructor);
		}

		if (!fields.isEmpty()) {
			MethodCompileContext fullArgConstructor = this.innerClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				fields
				.stream()
				.map((FieldCompileContext field) -> new LazyVarInfo(field.name(), field.info.type))
				.toArray(LazyVarInfo.ARRAY_FACTORY)
			);
			LazyVarInfo self = new LazyVarInfo("this", fullArgConstructor.clazz.info);
			invokeInstance(load(self), OBJECT_CONSTRUCTOR).emitBytecode(fullArgConstructor);
			for (FieldCompileContext field : fields) {
				putField(
					load(self),
					new FieldInfo(ACC_PUBLIC, this.innerClassType, field.name(), field.info.type),
					load(field.name(), field.info.type)
				)
				.emitBytecode(fullArgConstructor);
			}
			return_(noop).emitBytecode(fullArgConstructor);
			fullArgConstructor.endCode();
		}

		if (nonDefaulted.size() != fields.size() && !nonDefaulted.isEmpty()) {
			MethodCompileContext someArgsConstructor = this.innerClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				nonDefaulted
				.stream()
				.map((FieldCompileContext field) -> new LazyVarInfo(field.name(), field.info.type))
				.toArray(LazyVarInfo.ARRAY_FACTORY)
			);
			LazyVarInfo self = new LazyVarInfo("this", someArgsConstructor.clazz.info);
			invokeInstance(load(self), OBJECT_CONSTRUCTOR).emitBytecode(someArgsConstructor);
			for (FieldCompileContext field : fields) {
				putField(
					load(self),
					new FieldInfo(ACC_PUBLIC, this.innerClassType, field.info.name, field.info.type),
					field.initializer != null ? ldc(field.initializer) : load(field.name(), field.info.type)
				)
				.emitBytecode(someArgsConstructor);
			}
			return_(noop).emitBytecode(someArgsConstructor);
		}
	}

	public static void addToString(ClassCompileContext innerClass, String className, List<FieldCompileContext> fields) {
		MethodCompileContext toString = innerClass.newMethod(ACC_PUBLIC, "toString", TypeInfos.STRING);
		LazyVarInfo self = new LazyVarInfo("this", toString.clazz.info);
		StringBuilder pattern = new StringBuilder(className).append('(');
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
					fields.stream().map((FieldCompileContext field) -> field.info.type).toArray(TypeInfo.ARRAY_FACTORY)
				),
				new ConstantValue[] {
					constant(pattern.toString())
				},
				fields
				.stream()
				.map((FieldCompileContext field) -> getField(load(self), field.info))
				.toArray(InsnTree.ARRAY_FACTORY)
			)
		)
		.emitBytecode(toString);
		toString.endCode();
	}

	public void addHashCode(List<FieldCompileContext> fields) {
		MethodCompileContext hashCode = this.innerClass.newMethod(ACC_PUBLIC, "hashCode", TypeInfos.INT);
		LazyVarInfo self = new LazyVarInfo("this", hashCode.clazz.info);
		if (fields.isEmpty()) {
			return_(ldc(0)).emitBytecode(hashCode);
		}
		else {
			invokeStatic(
				HASH_MIX,
				ArrayExtensions.computeHashCode(
					getField(load(self), fields.get(0).info)
				)
			)
			.emitBytecode(hashCode);
			for (int index = 1, size = fields.size(); index < size; index++) {
				invokeStatic(
					HASH_MIX,
					add(
						this.parser,
						getFromStack(TypeInfos.INT),
						ArrayExtensions.computeHashCode(
							getField(load(self), fields.get(index).info)
						)
					)
				)
				.emitBytecode(hashCode);
			}
			return_(getFromStack(TypeInfos.INT)).emitBytecode(hashCode);
		}
		hashCode.endCode();
	}

	public void addEquals(List<FieldCompileContext> fields) {
		MethodCompileContext equals = this.innerClass.newMethod(ACC_PUBLIC, "equals", TypeInfos.BOOLEAN, new LazyVarInfo("object", TypeInfos.OBJECT));
		LazyVarInfo self = new LazyVarInfo("this", equals.clazz.info);
		LazyVarInfo object = new LazyVarInfo("object", TypeInfos.OBJECT);
		if (fields.isEmpty()) {
			return_(instanceOf(load(object), this.innerClassType)).emitBytecode(equals);
		}
		else {
			LazyVarInfo that = equals.scopes.addVariable("that", this.innerClassType);
			ifThen(
				not(condition(this.parser, instanceOf(load(object), this.innerClassType))),
				return_(ldc(false))
			)
			.emitBytecode(equals);
			store(that, load(object).cast(this.parser, this.innerClassType, CastMode.EXPLICIT_THROW)).emitBytecode(equals);
			for (FieldCompileContext field : fields) {
				ifThen(
					not(
						condition(
							this.parser,
							ArrayExtensions.computeEquals(
								this.parser,
								getField(load(self), field.info),
								getField(load(that), field.info)
							)
						)
					),
					return_(ldc(false))
				)
				.emitBytecode(equals);
			}
			return_(ldc(true)).emitBytecode(equals);
		}
		equals.endCode();
	}

	public void exposeToScript(List<FieldCompileContext> fields, List<FieldCompileContext> nonDefaulted) {
		this.parser.environment.user().types.put(this.className, this.innerClassType);
		this
		.parser
		.environment
		.user()
		.addConstructor(
			this.innerClassType,
			new MethodInfo(
				ACC_PUBLIC,
				this.innerClassType,
				"<init>",
				TypeInfos.VOID
			)
		);
		if (!fields.isEmpty()) {
			this
			.parser
			.environment
			.user()
			.addConstructor(
				this.innerClassType,
				new MethodInfo(
					ACC_PUBLIC,
					this.innerClassType,
					"<init>",
					TypeInfos.VOID,
					fields
					.stream()
					.map((FieldCompileContext field) -> field.info.type)
					.toArray(TypeInfo.ARRAY_FACTORY)
				)
			);
			if (nonDefaulted.size() != fields.size()) {
				this
				.parser
				.environment
				.user()
				.addConstructor(
					this.innerClassType,
					new MethodInfo(
						ACC_PUBLIC,
						this.innerClassType,
						"<init>",
						TypeInfos.VOID,
						nonDefaulted
						.stream()
						.map((FieldCompileContext field) -> field.info.type)
						.toArray(TypeInfo.ARRAY_FACTORY)
					)
				);
			}
		}
		fields.stream().map((FieldCompileContext field) -> field.info).forEach(this.parser.environment.user()::addFieldGetterAndSetter);
	}
}