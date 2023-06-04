package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DynamicRegistryEntryCoder<T> extends NamedCoder<RegistryEntry<T>> {

	public final DynamicRegistryCoder<T> registryCoder;

	public DynamicRegistryEntryCoder(DynamicRegistryCoder<T> registryCoder) {
		super("DynamicRegistryEntryCoder<" + registryCoder.registryKey.getValue() + '>');
		this.registryCoder = registryCoder;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Registry<T> lookup = context.decodeWith(this.registryCoder);
		Identifier id = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		RegistryKey<T> key = RegistryKey.of(this.registryCoder.registryKey, id);
		RegistryEntry<T> entry = lookup.getOrCreateEntry(key);
		if (entry != null) {
			return entry;
		}
		else {
			throw new DecodeException(id + " not present in registry " + this.registryCoder.registryKey.getValue());
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<T>> context) throws EncodeException {
		if (context.input == null) return context.empty();
		RegistryKey<T> key = context.input.getKey().orElse(null);
		if (key != null) {
			return context.input(key.getValue()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		}
		else {
			throw new EncodeException("RegistryEntry " + context.input + " is missing a key");
		}
	}
}