package builderb0y.bigglobe.columns.scripted.schemas;

public class BooleanAccessSchema extends PrimitiveAccessSchema {

	public BooleanAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.BOOLEAN;
	}
}