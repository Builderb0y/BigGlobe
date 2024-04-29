package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

@Mixin(DynamicRegistryManager.class)
public interface DynamicRegistryManager_LoadColumnEntryRegistry {

	@Inject(method = "createAndLoad", at = @At(value = "INVOKE_ASSIGN", shift = Shift.AFTER, target = "Lnet/minecraft/registry/DynamicRegistryManager;createMutableRegistryManager()Lnet/minecraft/registry/DynamicRegistryManager$Mutable;"), locals = LocalCapture.CAPTURE_FAILHARD)
	private static void bigglobe_beginLoad(CallbackInfoReturnable<DynamicRegistryManager.Mutable> callback, DynamicRegistryManager.Mutable mutable) {
		ColumnEntryRegistry.Loading.beginLoad(new BetterRegistry.Lookup() {

			@Override
			public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
				return new BetterHardCodedRegistry<>(mutable.get(key));
			}
		});
	}
}