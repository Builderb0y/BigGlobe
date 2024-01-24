package builderb0y.bigglobe.columns.scripted.schemas;

public class FloatAccessSchema extends PrimitiveAccessSchema {

	public FloatAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.FLOAT;
	}
}