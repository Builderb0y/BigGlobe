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

public class HardCodedRegistryEntryCoder<T> extends NamedCoder<RegistryEntry<T>> {

	public final Registry<T> registry;

	public HardCodedRegistryEntryCoder(Registry<T> registry) {
		super("HardCodedRegistryEntryCoder<" + registry.getKey().getValue() + '>');
		this.registry = registry;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Identifier identifier = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		RegistryKey<T> key = RegistryKey.of(this.registry.getKey(), identifier);
		RegistryEntry<T> entry = this.registry.getOrCreateEntry(key);
		if (entry != null) {
			return entry;
		}
		else {
			throw new DecodeException("Registry " + this.registry.getKey().getValue() + " does not contain ID " + identifier);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<T>> context) throws EncodeException {
		RegistryEntry<T> input = context.input;
		if (input == null) return context.empty();
		RegistryKey<T> key = input.getKey().orElse(null);
		if (key != null) {
			return context.input(key.getValue()).encodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		}
		else {
			throw new EncodeException("Registry " + this.registry.getKey().getValue() + " does not contain object " + input);
		}
	}
}