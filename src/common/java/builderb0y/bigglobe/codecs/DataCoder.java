package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class DataCoder extends NamedCoder<Data> {

	public static final DataCoder INSTANCE = new DataCoder("DataCoder.INSTANCE");

	public DataCoder(String name) {
		super(name);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable Data decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return context.data;
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, Data> context) throws EncodeException {
		Data data = context.object;
		return data != null ? data : EmptyData.INSTANCE;
	}
}