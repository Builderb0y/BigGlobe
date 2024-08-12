package builderb0y.bigglobe.mixins;

import me.cortex.voxy.client.saver.ContextSelectionSystem;
import me.cortex.voxy.common.storage.StorageBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.ForgetfulMemoryStorageBackend;

@Mixin(ContextSelectionSystem.Selection.class)
public class Voxy_ContextSelectionSystem_UseMemoryStorageBackendForDebugging {

	@Inject(method = "createStorageBackend", at = @At("HEAD"), cancellable = true, remap = false)
	private void bigglobe_useMemoryStorageBackendForDebugging(CallbackInfoReturnable<StorageBackend> callback) {
		if (AbstractVoxyWorldGenerator.override != null) {
			callback.setReturnValue(new ForgetfulMemoryStorageBackend());
		}
	}
}