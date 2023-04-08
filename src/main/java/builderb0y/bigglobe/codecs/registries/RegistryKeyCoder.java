package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class RegistryKeyCoder<T> extends NamedCoder<RegistryKey<T>>  {

	public final RegistryKey<Registry<T>> registryKey;

	public RegistryKeyCoder(RegistryKey<Registry<T>> registryKey) {
		super("RegistryKeyCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@Override
	public <T_Encoded> @Nullable RegistryKey<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		return RegistryKey.of(this.registryKey, context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER));
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryKey<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		return context.input(context.input.getValue()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
	}
}