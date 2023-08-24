package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.*;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

#if MC_VERSION > MC_1_19_2
import net.minecraft.registry.entry.RegistryEntryOwner;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterDynamicRegistry;
#endif

public class BetterDynamicRegistryCoder<T> extends NamedCoder<BetterRegistry<T>> {

	public final RegistryKey<Registry<T>> registryKey;

	public BetterDynamicRegistryCoder(RegistryKey<Registry<T>> key) {
		super("BetterDynamicRegistryCoder<" + key.getValue() + '>');
		this.registryKey = key;
	}

	@Override
	public <T_Encoded> @Nullable BetterRegistry<T> decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			#if MC_VERSION == MC_1_19_2
				Registry<T> registry = registryOps.getRegistry(this.registryKey).orElse(null);
				if (registry == null) {
					throw new DecodeException(() -> "Registry " + this.registryKey.getValue() + " not present in RegistryOps");
				}
				return new BetterHardCodedRegistry<>(registry);
			#else
				RegistryEntryLookup<T> lookup = registryOps.getEntryLookup(this.registryKey).orElse(null);
				if (lookup == null) {
					throw new DecodeException(() -> "Registry " + this.registryKey.getValue() + " not present in RegistryOps");
				}
				RegistryEntryOwner<T> owner = registryOps.getOwner(this.registryKey).orElse(null);
				if (!(owner instanceof RegistryWrapper.Impl<T> wrapperImpl)) {
					throw new DecodeException(() -> "Owner is not a RegistryWrapper.Impl: " + owner + " in registry " + this.registryKey.getValue());
				}
				return new BetterDynamicRegistry<>(wrapperImpl, lookup);
			#endif
		}
		else {
			throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BetterRegistry<T>> context) throws EncodeException {
		return context.empty();
	}
}