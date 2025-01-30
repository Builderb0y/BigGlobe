package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.thread.ServiceThreadPool;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.mixinInterfaces.VoxyGeneratorHolder;

@Mixin(WorldEngine.class)
public class Voxy_WorldEngine_UseBigGlobeGenerator implements VoxyGeneratorHolder {

	public AbstractVoxyWorldGenerator bigglobe_generator;

	@Inject(method = "<init>(Lme/cortex/voxy/common/storage/StorageBackend;Lme/cortex/voxy/common/thread/ServiceThreadPool;I)V", at = @At("RETURN"), remap = false)
	private void bigglobe_startGenerator(
		StorageBackend storageBackend,
		#if MC_VERSION >= MC_1_21_2
			ServiceThreadPool serviceThreadPool,
		#else
			int ingestWorkers,
			int savingServiceWorkers,
		#endif
		int maxMipLayers,
		CallbackInfo callback
	) {
		this.bigglobe_generator = AbstractVoxyWorldGenerator.createGenerator(
			MinecraftClient.getInstance().world,
			(WorldEngine)(Object)(this)
		);

		#if MC_VERSION < MC_1_21_2
			if (this.bigglobe_generator != null) {
				this.bigglobe_generator.start();
			}
		#endif
	}

	#if MC_VERSION < MC_1_21_2

		@Inject(method = "shutdown", at = @At("HEAD"), remap = false)
		private void bigglobe_stopGenerator(CallbackInfo callback) {
			AbstractVoxyWorldGenerator generator = this.bigglobe_generator;
			if (generator != null) {
				generator.stop();
			}
		}

	#endif

	@Override
	public AbstractVoxyWorldGenerator bigglobe_getVoxyGenerator() {
		return this.bigglobe_generator;
	}
}