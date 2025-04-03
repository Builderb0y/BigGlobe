package builderb0y.bigglobe.mixins;

import me.cortex.voxy.client.core.rendering.RenderService;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend;

@Mixin(RenderService.class)
public class Voxy_RenderService_ShutdownHook {

	@Shadow(remap = false) @Final private WorldEngine world;

	@Inject(method = "shutdown", at = @At("HEAD"), remap = false)
	private void bigglobe_shutdownVoxyWorldgenThread(CallbackInfo callback) {
		if (this.world.storage instanceof GeneratingStorageBackend generating && generating.generator != null) {
			generating.generator.stop();
		}
	}
}