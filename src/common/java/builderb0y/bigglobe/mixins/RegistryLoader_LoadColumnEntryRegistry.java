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
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.resources.RegistryOps.RegistryInfo;
#endif

@Mixin(RegistryDataLoader.class)
public class RegistryLoader_LoadColumnEntryRegistry {

	@ModifyReturnValue(method = "createContext", at = @At("RETURN"))
	private static RegistryInfoLookup bigglobe_beginLoading(RegistryInfoLookup lookup) {
		ColumnEntryRegistry.Loading.beginLoad(new RegistryAdapter.Lookup() {

			@Override
			public <T> RegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key) {
				HolderGetter<T> getter = lookup.lookup(key).orElse(null);
				if (getter == null) {
					throw new IllegalStateException("Missing registry: " + key.identifier());
				}
				return new RegistryAdapter.BasicImpl<>(key, getter);
			}
		});
		return lookup;
	}
}