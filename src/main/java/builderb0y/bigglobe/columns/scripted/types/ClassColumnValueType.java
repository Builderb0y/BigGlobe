package builderb0y.bigglobe.columns.scripted.types;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Unit;

import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.CustomClassCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.MemberKeywordHandler;
import builderb0y.scripting.environments.ScriptEnvironment.MemberKeywordMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues;
import builderb0y.scripting.parsing.SpecialFunctionSyntax.NamedValues.NamedValue;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassColumnValueType implements ColumnValueType {

	public final String name;
	public final Map<@IdentifierName String, ColumnValueType> fields;
	public final transient ClassColumnValueField[] fieldsInOrder;

	public ClassColumnValueType(String name, Map<@IdentifierName String, ColumnValueType> fields) {
		this.name = name;
		this.fields = fields;
		//order is arbitrary as long as it's consistent.
		this.fieldsInOrder = fields.entrySet().stream().map((Map.Entry<String, ColumnValueType> entry) -> new ClassColumnValueField(entry.getKey(), entry.getValue())).toArray(ClassColumnValueField[]::new);
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		CustomClassCompileContext customClassContext = new CustomClassCompileContext(context, this);
		return new TypeContext(customClassContext.selfType(), customClassContext);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		CustomClassCompileContext selfContext = (CustomClassCompileContext)(context.getTypeContext(this).context());
		if (object == Unit.INSTANCE) return ldc(null, selfContext.selfType());
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)(object);
		Map<String, InsnTree> constants = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, Object> entry) -> {
			ColumnValueType type = this.fields.get(entry.getKey());
			if (type == null) throw new IllegalArgumentException("Undeclared field specified: " + entry.getKey());
			return type.createConstant(entry.getValue(), context);
		}));
		int length = this.fieldsInOrder.length;
		InsnTree[] args = new InsnTree[length];
		for (int index = 0; index < length; index++) {
			String name = this.fieldsInOrder[index].name;
			InsnTree constant = constants.get(name);
			if (constant == null) throw new IllegalArgumentException("Unspecified field: " + name);
			args[index] = constant;
		}
		return newInstance(selfContext.constructor.info, args);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, TypeContext typeContext, DataCompileContext context, ColumnValueDependencyHolder dependencies) {
		TypeInfo type = typeContext.type();
		environment.addType(this.name, type);
		for (Map.Entry<String, ColumnValueType> entry : this.fields.entrySet()) {
			environment.addFieldGet(
				new FieldInfo(
					ACC_PUBLIC,
					type,
					entry.getKey(),
					context.root().getTypeContext(entry.getValue()).type()
				)
			);
		}
		MethodInfo constructor = new MethodInfo(
			ACC_PUBLIC,
			type,
			"<init>",
			TypeInfos.VOID,
			Arrays
			.stream(this.fieldsInOrder)
			.map(ClassColumnValueField::type)
			.map(context.root()::getTypeContext)
			.map(TypeContext::type)
			.toArray(TypeInfo.ARRAY_FACTORY)
		);
		environment.addMemberKeyword(TypeInfos.CLASS, "new", new MemberKeywordHandler.Named("Constructor for " + this.name, (ExpressionParser parser, InsnTree receiver, String theStringNew, MemberKeywordMode mode) -> {
			if (!receiver.getConstantValue().isConstant() || !receiver.getConstantValue().asJavaObject().equals(type)) return null;
			NamedValues namedValues = NamedValues.parse(parser, null, (ExpressionParser theSameParser, String name) -> {
				if (!this.fields.containsKey(name)) {
					throw new ScriptParsingException("Unknown field: " + name + "; valid fields are: " + this.fields, theSameParser.input);
				}
			});
			Map<String, InsnTree> lookup = Arrays.stream(namedValues.values()).collect(Collectors.toMap(NamedValue::name, NamedValue::value));
			InsnTree[] args = new InsnTree[constructor.paramTypes.length];
			ClassColumnValueField[] parameters = this.fieldsInOrder;
			for (int index = 0, size = parameters.length; index < size; index++) {
				String name = parameters[index].name();
				InsnTree tree = lookup.get(name);
				if (tree == null) throw new ScriptParsingException("Must specify " + name, parser.input);
				args[index] = tree.cast(parser, constructor.paramTypes[index], CastMode.IMPLICIT_THROW);
			}
			//todo: create synthetic permute method to preserve left-to-right evaluation order.
			return newInstance(constructor, args);
		}));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.fields.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ClassColumnValueType that &&
			this.name.equals(that.name) &&
			this.fields.equals(that.fields)
		);
	}

	@Override
	public String toString() {
		return "{ type: class, name: " + this.name + ", fields: " + this.fields + " }";
	}

	public static record ClassColumnValueField(String name, ColumnValueType type) {}
}