package builderb0y.bigglobe.columns.scripted.schemas;

public class ByteAccessSchema extends PrimitiveAccessSchema {

	public ByteAccessSchema(boolean is_3d) {
		super(is_3d);
	}

	@Override
	public PrimitiveType primitiveType() {
		return PrimitiveType.BYTE;
	}
}