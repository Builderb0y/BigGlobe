package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.tag.TagKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class TagKeyCoder<T> extends NamedCoder<TagKey<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public TagKeyCoder(RegistryKey<Registry<T>> registryKey) {
		super("TagKeyCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@Override
	public <T_Encoded> @Nullable TagKey<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		return TagKey.of(this.registryKey, context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER));
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, TagKey<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		return context.input(context.input.id()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
	}
}