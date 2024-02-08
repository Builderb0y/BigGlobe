package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class DecoderContextCoder extends NamedCoder<DecodeContext> {

	public static final DecoderContextCoder INSTANCE = new DecoderContextCoder("DecoderContextCoder.INSTANCE");

	public DecoderContextCoder(String name) {
		super(name);
	}

	@Override
	public @Nullable <T_Encoded> DecodeContext<?> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return context;
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, DecodeContext> context) throws EncodeException {
		return context.empty();
	}
}