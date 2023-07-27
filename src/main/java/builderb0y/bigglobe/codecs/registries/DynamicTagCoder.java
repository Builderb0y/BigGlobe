package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryEntryListVersions;

public class DynamicTagCoder<T> extends NamedCoder<RegistryEntryList<T>> {

	public final DynamicRegistryCoder<T> registryCoder;

	public DynamicTagCoder(DynamicRegistryCoder<T> registryCoder) {
		super("DynamicTagCoder<" + registryCoder.registryKey.getValue() + '>');
		this.registryCoder = registryCoder;
	}

	@Override
	public @Nullable <T_Encoded> RegistryEntryList<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		RegistryEntryLookup<T> lookup = context.decodeWith(this.registryCoder);
		Identifier tagID = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		TagKey<T> tagKey = TagKey.of(this.registryCoder.registryKey, tagID);
		RegistryEntryList<T> tag = lookup.getOptional(tagKey).orElse(null);
		if (tag != null) {
			return tag;
		}
		else {
			throw new DecodeException(() -> "Tag " + tagID + " not present in registry " + lookup);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntryList<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		TagKey<T> key = RegistryEntryListVersions.getKeyNullable(context.input);
		if (key != null) {
			return context.input(key.id()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		}
		else {
			throw new EncodeException(() -> "Tag " + context.input + " is missing a key");
		}
	}
}