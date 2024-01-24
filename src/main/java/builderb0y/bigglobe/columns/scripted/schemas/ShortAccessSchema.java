package builderb0y.bigglobe.columns.scripted.schemas;

public class ShortAccessSchema extends PrimitiveAccessSchema {

	public ShortAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.SHORT;
	}
}