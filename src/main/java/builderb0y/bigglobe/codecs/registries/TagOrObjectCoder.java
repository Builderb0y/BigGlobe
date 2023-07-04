package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.coders.PrimitiveCoders;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.versions.AutoCodecVersions;

public class TagOrObjectCoder<T> extends NamedCoder<TagOrObject<T>> {

	public final AutoCoder<RegistryEntryList<T>> tagCoder;
	public final AutoCoder<RegistryEntry<T>> entryCoder;

	public TagOrObjectCoder(RegistryKey<Registry<T>> registryKey, AutoCoder<RegistryEntryList<T>> tagCoder, AutoCoder<RegistryEntry<T>> entryCoder) {
		super("TagOrObjectCoder<" + registryKey.getValue() + '>');
		this.tagCoder = tagCoder;
		this.entryCoder = entryCoder;
	}

	@Override
	public @Nullable <T_Encoded> TagOrObject<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String string = context.forceAsString();
		if (!string.isEmpty() && string.charAt(0) == '#') {
			return new TagOrObject<>(context.input(context.createString(string.substring(1))).decodeWith(this.tagCoder));
		}
		else {
			return new TagOrObject<>(context.decodeWith(this.entryCoder));
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, TagOrObject<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		return context.input(context.input.toString(AutoCodecVersions::newEncodeException)).encodeWith(PrimitiveCoders.STRING);
	}
}