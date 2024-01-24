package builderb0y.bigglobe.columns.scripted.schemas;

public class DoubleAccessSchema extends PrimitiveAccessSchema {

	public DoubleAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.DOUBLE;
	}
}