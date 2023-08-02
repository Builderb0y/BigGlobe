package builderb0y.bigglobe.mixins;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;

@Mixin(DynamicRegistryManager.class)
public interface DynamicRegistryManager_AddBigGlobeInfos {

	@Dynamic("lambda method")
	@Inject(method = "method_30531()Lcom/google/common/collect/ImmutableMap;", at = @At(value = "INVOKE_ASSIGN", target = "com/google/common/collect/ImmutableMap.builder()Lcom/google/common/collect/ImmutableMap$Builder;"), locals = LocalCapture.CAPTURE_FAILHARD)
	private static void bigglobe_addInfos(
		CallbackInfoReturnable<
			ImmutableMap<
				RegistryKey<? extends Registry<?>>,
				DynamicRegistryManager.Info<?>
			>
		>
		callback,
		ImmutableMap.Builder<
			RegistryKey<? extends Registry<?>>,
			DynamicRegistryManager.Info<?>
		>
		builder
	) {
		BigGlobeDynamicRegistries.registerBigGlobeDynamicRegistries(builder);
	}
}