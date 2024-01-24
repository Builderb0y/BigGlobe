package builderb0y.bigglobe.columns.scripted.schemas;

public class LongAccessSchema extends PrimitiveAccessSchema {

	public LongAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.LONG;
	}
}