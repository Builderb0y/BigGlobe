package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.config.section.SectionStorage;
import me.cortex.voxy.common.thread.ServiceThreadPool;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend;

@Mixin(value = WorldEngine.class, remap = false)
public class Voxy_WorldEngine_UseBigGlobeGenerator {

	@Inject(method = "<init>(Lme/cortex/voxy/common/config/section/SectionStorage;Lme/cortex/voxy/common/thread/ServiceThreadPool;II)V", at = @At("RETURN"))
	private void bigglobe_startGenerator(
		SectionStorage storage,
		ServiceThreadPool serviceThreadPool,
		int maxMipLayers,
		int cacheCount,
		CallbackInfo callback
	) {
		if (storage instanceof GeneratingStorageBackend generating) {
			generating.generator = (
				AbstractVoxyWorldGenerator.createGenerator(
					MinecraftClient.getInstance().world,
					(WorldEngine)(Object)(this)
				)
			);
		}
	}
}