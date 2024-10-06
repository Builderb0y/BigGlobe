package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.registry.*;
import net.minecraft.registry.RegistryOps.RegistryInfo;
import net.minecraft.registry.RegistryOps.RegistryInfoGetter;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterDynamicRegistry;

@Mixin(RegistryLoader.class)
public class RegistryLoader_LoadColumnEntryRegistry {

	@ModifyReturnValue(method = "createInfoGetter", at = @At("RETURN"))
	private static RegistryInfoGetter bigglobe_beginLoading(RegistryInfoGetter getter) {
		ColumnEntryRegistry.Loading.beginLoad(new BetterRegistry.Lookup() {

			@Override
			public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
				RegistryInfo<T> info = getter.getRegistryInfo(key).orElse(null);
				if (info == null) {
					throw new IllegalStateException("Missing registry: " + key.getValue());
				}
				RegistryEntryLookup<T> lookup = info.entryLookup();
				if (!(info.owner() instanceof RegistryWrapper.Impl<T> impl)) {
					throw new IllegalStateException("Owner is not a RegistryWrapper.Impl: " + info.owner() + " in registry " + key.getValue());
				}
				return new BetterDynamicRegistry<>(impl, lookup);
			}
		});
		return getter;
	}
}