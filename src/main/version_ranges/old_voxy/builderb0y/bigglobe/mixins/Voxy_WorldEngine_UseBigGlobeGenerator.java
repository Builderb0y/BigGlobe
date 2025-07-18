package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;

@Mixin(value = WorldEngine.class, remap = false)
public class Voxy_WorldEngine_UseBigGlobeGenerator {

	@Unique
	public AbstractVoxyWorldGenerator bigglobe_generator;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_startGenerator(
		StorageBackend storageBackend,
		int ingestWorkers,
		int savingServiceWorkers,
		int maxMipLayers,
		CallbackInfo callback
	) {
		this.bigglobe_generator = AbstractVoxyWorldGenerator.createGenerator(
			MinecraftClient.getInstance().world,
			(WorldEngine)(Object)(this)
		);

		if (this.bigglobe_generator != null) {
			this.bigglobe_generator.start();
		}
	}

	@Inject(method = "shutdown", at = @At("HEAD"))
	private void bigglobe_stopGenerator(CallbackInfo callback) {
		AbstractVoxyWorldGenerator generator = this.bigglobe_generator;
		if (generator != null) {
			generator.stop();
		}
	}
}