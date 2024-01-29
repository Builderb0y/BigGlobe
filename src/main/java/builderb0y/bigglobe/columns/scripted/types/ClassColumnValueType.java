package builderb0y.bigglobe.columns.scripted.types;

import java.util.Map;
import java.util.stream.Collectors;

import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.CustomClassCompileContext;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

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
		CustomClassCompileContext selfContext = (CustomClassCompileContext)(context.getTypeContext(this).context());
		return newInstance(selfContext.constructor.info, args);
	}

	@Override
	public void setupExternalEnvironment(TypeContext typeContext, ColumnCompileContext context, MutableScriptEnvironment environment) {
		environment.addType(this.name, typeContext.type());
		for (Map.Entry<String, ColumnValueType> entry : this.fields.entrySet()) {
			environment.addFieldGet(
				new FieldInfo(
					ACC_PUBLIC,
					typeContext.type(),
					entry.getKey(),
					context.getTypeContext(entry.getValue()).type()
				)
			);
		}
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