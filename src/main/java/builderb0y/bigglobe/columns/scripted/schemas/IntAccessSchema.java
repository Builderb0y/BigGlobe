package builderb0y.bigglobe.columns.scripted.schemas;

public class IntAccessSchema extends PrimitiveAccessSchema {

	public IntAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.INT;
	}
}