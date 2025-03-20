package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.config.section.SectionStorage;
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

@Mixin(value = WorldEngine.class, remap = false)
public class Voxy_WorldEngine_UseBigGlobeGenerator {

	@Shadow @Final public SectionStorage storage;

	@Inject(method = "<init>(Lme/cortex/voxy/common/config/section/SectionStorage;II)V", at = @At("RETURN"))
	private void bigglobe_startGenerator(
		SectionStorage storage,
		int maxMipLayers,
		int cacheCount,
		CallbackInfo callback
	) {
		if (false && storage instanceof GeneratingStorageBackend generating) {
			generating.generator = (
				AbstractVoxyWorldGenerator.createGenerator(
					MinecraftClient.getInstance().world,
					(WorldEngine)(Object)(this)
				)
			);
			if (generating.generator != null) {
				generating.generator.start();
			}
		}
	}

	@Inject(method = "free", at = @At("HEAD"))
	private void bigglobe_shutdownGenerator(CallbackInfo callback) {
		if (this.storage instanceof GeneratingStorageBackend generating && generating.generator != null) {
			generating.generator.stop();
		}
	}
}