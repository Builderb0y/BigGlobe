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

public class HardCodedTagCoder<T> extends NamedCoder<RegistryEntryList<T>> {

	public final Registry<T> registry;

	public HardCodedTagCoder(Registry<T> registry) {
		super("HardCodedTagCoder<" + registry.getKey().getValue() + '>');
		this.registry = registry;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntryList<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Identifier tagID = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		TagKey<T> tagKey = TagKey.of(this.registry.getKey(), tagID);
		RegistryEntryList<T> tag = this.registry.getOrCreateEntryList(tagKey);
		if (tag != null) {
			return tag;
		}
		else {
			throw new DecodeException("No such tag " + tagID + " in registry " + this.registry.getKey());
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