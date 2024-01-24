package builderb0y.bigglobe.columns.scripted.schemas;

import java.util.Map;

import org.objectweb.asm.tree.ParameterNode;

import builderb0y.bigglobe.columns.scripted.MappedRangeObjectArray;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.CustomClassCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassAccessSchema implements AccessSchema {

	public final @IdentifierName String name;
	public final Map<@IdentifierName String, AccessSchema> fields;
	public final boolean is_3d;

	public ClassAccessSchema(String name, Map<String, AccessSchema> fields, boolean is_3d) {
		this.name = name;
		this.fields = fields;
		this.is_3d = is_3d;
	}

	@Override
	public boolean is3D() {
		return this.is_3d;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		CustomClassCompileContext newContext = new CustomClassCompileContext(context, this);
		return new TypeContext(newContext.mainClass.info, this.is3D() ? type(MappedRangeObjectArray.class) : newContext.mainClass.info, newContext);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		CustomClassCompileContext customContext = (CustomClassCompileContext)(context.getSchemaType(this).context());
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>)(object);
		return newInstance(
			customContext.constructor.info,
			customContext
			.constructor
			.node
			.parameters
			.stream()
			.map((ParameterNode node) -> node.name)
			.map((String name) -> {
				AccessSchema schema = this.fields.get(name);
				if (schema == null) throw new IllegalStateException("No schema for " + name);
				Object value = map.get(name);
				if (value == null) throw new IllegalStateException("No map entry for " + name);
				return schema.createConstant(value, context);
			})
			.toArray(InsnTree.ARRAY_FACTORY)
		);
	}

	@Override
	public int hashCode() {
		int hash = this.name.hashCode();
		hash = hash * 31 + this.fields.hashCode();
		hash = hash * 31 + Boolean.hashCode(this.is_3d);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ClassAccessSchema that &&
			this.name.equals(that.name) &&
			this.fields.equals(that.fields) &&
			this.is_3d == that.is_3d
		);
	}
}