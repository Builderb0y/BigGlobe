package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.cortex.voxy.client.saver.ContextSelectionSystem;
import me.cortex.voxy.common.storage.StorageBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.ForgetfulMemoryStorageBackend;
import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(ContextSelectionSystem.Selection.class)
public class Voxy_ContextSelectionSystem_UseMemoryStorageBackendForDebugging {

	@Inject(method = "createStorageBackend", at = @At("HEAD"), cancellable = true, remap = false)
	private void bigglobe_useForgetfulStorageBackendWhenDebugging(CallbackInfoReturnable<StorageBackend> callback) {
		if (AbstractVoxyWorldGenerator.override != null) {
			callback.setReturnValue(new ForgetfulMemoryStorageBackend());
		}
	}

	@ModifyReturnValue(method = "createStorageBackend", at = @At("RETURN"), remap = false)
	private StorageBackend bigglobe_useGeneratingStorageBackend(StorageBackend original) {
		if (BigGlobeConfig.INSTANCE.get().voxyIntegration.useWorldgenThread) {
			return new GeneratingStorageBackend(original);
		}
		else {
			return original;
		}
	}
}