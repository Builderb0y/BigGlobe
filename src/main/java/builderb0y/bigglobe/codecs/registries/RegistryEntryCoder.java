package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class RegistryEntryCoder<T> extends AbstractRegistryCoder<T, RegistryEntry<T>> {

	public RegistryEntryCoder(@NotNull ReifiedType<RegistryEntry<T>> handledType, RegistryKey<Registry<T>> key) {
		super(handledType, key);
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable RegistryEntry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		return this.registry(context).getByName(context.forceAsString());
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, RegistryEntry<T>> context) throws EncodeException {
		RegistryEntry<T> entry = context.object;
		if (entry == null) return context.empty();
		RegistryKey<T> key = entry.getKey().orElse(null);
		if (key != null) return context.createString(key.getValue().toString());
		else throw new EncodeException(() -> "Unregistered object: " + entry);
	}
}