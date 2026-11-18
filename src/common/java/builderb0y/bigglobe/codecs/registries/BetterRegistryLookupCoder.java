package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

public class BetterRegistryLookupCoder extends NamedCoder<BetterRegistry.Lookup> {

	public static final BetterRegistryLookupCoder INSTANCE = new BetterRegistryLookupCoder("BetterRegistryLookupCoder.INSTANCE");

	public BetterRegistryLookupCoder(@NotNull String toString) {
		super(toString);
	}

	@Override
	public <T_Encoded> BetterRegistry.@Nullable Lookup decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			return fromOps(registryOps);
		}
		else {
			throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
		}
	}

	public static BetterRegistry.Lookup fromOps(RegistryOps<?> registryOps) {
		return new BetterRegistry.Lookup() {

			@Override
			public <T> BetterRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
				HolderGetter<T> getter = registryOps.getter(key).orElse(null);
				if (getter == null) {
					throw new IllegalStateException("Missing registry: " + key.identifier());
				}
				#if MC_VERSION >= MC_26_3_0
					if (!(getter instanceof HolderLookup.RegistryLookup<T> full)) {
						throw new IllegalStateException("HolderGetter is not a RegistryLookup: " + getter + " in registry " + key.identifier());
					}
					return new BetterHardCodedRegistry<>(full);
				#else
					HolderOwner<T> owner = registryOps.owner(key).orElse(null);
					if (!(owner instanceof HolderLookup.RegistryLookup<T> impl)) {
						throw new IllegalStateException("Owner is not a RegistryWrapper.Impl: " + owner + " in registry " + key.identifier());
					}
					return new BetterRegistry.BetterDynamicRegistry<>(impl, getter);
				#endif
			}
		};
	}

	@Override
	public <T_Encoded> Data encode(@NotNull EncodeContext<T_Encoded, BetterRegistry.Lookup> context) throws EncodeException {
		return EmptyData.INSTANCE;
	}
}