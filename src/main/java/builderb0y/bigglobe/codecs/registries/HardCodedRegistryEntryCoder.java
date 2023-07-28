package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryVersions;

public class HardCodedRegistryEntryCoder<T> extends NamedCoder<RegistryEntry<T>> {

	public final Registry<T> registry;

	public HardCodedRegistryEntryCoder(Registry<T> registry) {
		super("HardCodedObjectCoder<" + RegistryVersions.getRegistryKey(registry).getValue() + '>');
		this.registry = registry;
	}

	@Override
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		Identifier identifier = context.decodeWith(BigGlobeAutoCodec.IDENTIFIER_CODER);
		RegistryKey<T> key = RegistryKey.of(RegistryVersions.getRegistryKey(this.registry), identifier);
		RegistryEntry<T> entry = this.registry.getEntry(key).orElse(null);
		if (entry != null) {
			return entry;
		}
		else {
			throw new DecodeException(() -> "Registry " + RegistryVersions.getRegistryKey(this.registry).getValue() + " does not contain ID " + identifier);
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
			throw new EncodeException(() -> "Registry " + RegistryVersions.getRegistryKey(this.registry).getValue() + " does not contain object " + input);
		}
	}
}