package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.util.TagOrObjectKey;

public class TagOrObjectKeyCoder<T> extends NamedCoder<TagOrObjectKey<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public TagOrObjectKeyCoder(RegistryKey<Registry<T>> registryKey) {
		super("TagOrObjectKeyCoder<" + registryKey.getValue() + '>');
		this.registryKey = registryKey;
	}

	@Override
	public <T_Encoded> @Nullable TagOrObjectKey<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String string = context.forceAsString();
		if (!string.isEmpty() && string.charAt(0) == '#') {
			return new TagOrObjectKey<>(TagKey.of(this.registryKey, context.input(context.createString(string.substring(1))).decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER)));
		}
		else {
			return new TagOrObjectKey<>(RegistryKey.of(this.registryKey, context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER)));
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, TagOrObjectKey<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		return context.createString(context.input.toString());
	}
}