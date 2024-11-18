package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.Map;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.registry.*;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.registry.RegistryOps.RegistryInfo;
import net.minecraft.registry.RegistryOps.RegistryInfoGetter;
import net.minecraft.resource.ResourceManager;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterDynamicRegistry;

@Mixin(RegistryLoader.class)
public class RegistryLoader_LoadColumnEntryRegistry {

	@Inject(
		method = "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/DynamicRegistryManager;Ljava/util/List;)Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
		at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", ordinal = 0)
	)
	private static void bigglobe_beginLoading(
		ResourceManager resourceManager,
		DynamicRegistryManager baseRegistryManager,
		List<RegistryLoader.Entry<?>> entries,
		CallbackInfoReturnable<Immutable> callback,
		@Local RegistryInfoGetter registryInfoGetter
	) {
		ColumnEntryRegistry.Loading.beginLoad(new BetterRegistry.Lookup() {

			@Override
			public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
				RegistryInfo<T> info = registryInfoGetter.getRegistryInfo(key).orElse(null);
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
	}
}