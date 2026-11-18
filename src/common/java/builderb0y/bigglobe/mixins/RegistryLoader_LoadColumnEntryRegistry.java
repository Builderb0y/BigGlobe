package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps.RegistryInfoLookup;
import net.minecraft.resources.ResourceKey;

import builderb0y.bigglobe.columns.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.resources.RegistryOps.RegistryInfo;
#endif

@Mixin(RegistryDataLoader.class)
public class RegistryLoader_LoadColumnEntryRegistry {

	@ModifyReturnValue(method = "createContext", at = @At("RETURN"))
	private static RegistryInfoLookup bigglobe_beginLoading(RegistryInfoLookup lookup) {
		ColumnEntryRegistry.Loading.beginLoad(new BetterRegistry.Lookup() {

			@Override
			public <T> BetterRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
				#if MC_VERSION >= MC_26_3_0
					HolderGetter<T> getter = lookup.lookup(key).orElse(null);
					if (getter == null) {
						throw new IllegalStateException("Missing registry: " + key.identifier());
					}
					if (!(getter instanceof HolderLookup.RegistryLookup<T> impl)) {
						throw new IllegalStateException("Getter is not a RegistryLookup: " + getter + " in registry " + key.identifier());
					}
					return new BetterHardCodedRegistry<>(impl);
				#else
					RegistryInfo<T> getter = lookup.lookup(key).orElse(null);
					if (getter == null) {
						throw new IllegalStateException("Missing registry: " + key.identifier());
					}
					HolderGetter<T> lookup = info.getter();
					if (!(info.owner() instanceof HolderLookup.RegistryLookup<T> impl)) {
						throw new IllegalStateException("Owner is not a RegistryWrapper.Impl: " + info.owner() + " in registry " + key.identifier());
					}
					return new BetterDynamicRegistry<>(impl, getter);
				#endif
			}
		});
		return lookup;
	}
}