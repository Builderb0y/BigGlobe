package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntryList;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DynamicTagCoder<T> extends NamedCoder<RegistryEntryList<T>> {

	public final DynamicRegistryCoder<T> registryCoder;

	public DynamicTagCoder(DynamicRegistryCoder<T> registryCoder) {
		super("DynamicTagCoder<" + registryCoder.registryKey.getValue() + '>');
		this.registryCoder = registryCoder;
	}

	@Override
	public @Nullable <T_Encoded> RegistryEntryList<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Registry<T> lookup = context.decodeWith(this.registryCoder);
		Identifier tagID = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		TagKey<T> tagKey = TagKey.of(this.registryCoder.registryKey, tagID);
		RegistryEntryList<T> tag = lookup.getOrCreateEntryList(tagKey);
		if (tag != null) {
			return tag;
		}
		else {
			throw new DecodeException("Tag " + tagID + " not present in registry " + lookup);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntryList<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		TagKey<T> key = context.input.getStorage().left().orElse(null);
		if (key != null) {
			return context.input(key.id()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		}
		else {
			throw new EncodeException("Tag " + context.input + " is missing a key");
		}
	}
}