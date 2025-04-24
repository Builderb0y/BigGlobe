package builderb0y.bigglobe.mixins;

import me.cortex.voxy.client.core.rendering.VoxyRenderSystem;
import me.cortex.voxy.common.thread.ServiceThreadPool;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend;

@Mixin(VoxyRenderSystem.class)
public class VoxyRenderSystem_ManageGenerator {

	@Shadow(remap = false) @Final private WorldEngine worldIn;

	@Inject(method = "<init>", at = @At("TAIL"), remap = false)
	private void bigglobe_initGenerator(WorldEngine world, ServiceThreadPool threadPool, CallbackInfo callback) {
		if (world.storage instanceof GeneratingStorageBackend generating) {
			generating.generator = (
				AbstractVoxyWorldGenerator.createGenerator(
					MinecraftClient.getInstance().world,
					world
				)
			);
			if (generating.generator != null) {
				generating.generator.start();
			}
		}
	}

	@Inject(method = "shutdown", at = @At("HEAD"), remap = false)
	private void bigglobe_shutdownVoxyWorldgenThread(CallbackInfo callback) {
		if (this.worldIn.storage instanceof GeneratingStorageBackend generating && generating.generator != null) {
			generating.generator.stop();
		}
	}
}