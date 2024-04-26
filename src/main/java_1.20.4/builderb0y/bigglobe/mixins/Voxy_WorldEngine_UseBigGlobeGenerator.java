package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.compat.voxy.VoxyWorldGenerator;

@Mixin(WorldEngine.class)
public class Voxy_WorldEngine_UseBigGlobeGenerator {

	public VoxyWorldGenerator bigglobe_generator;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void bigglobe_startGenerator(
		StorageBackend storageBackend,
		int ingestWorkers,
		int savingServiceWorkers,
		int maxMipLayers,
		CallbackInfo callback
	) {
		VoxyWorldGenerator generator = VoxyWorldGenerator.createGenerator(MinecraftClient.getInstance().world, ((WorldEngine)(Object)(this)));
		if (generator != null) {
			(this.bigglobe_generator = generator).start();
		}
	}

	@Inject(method = "shutdown", at = @At("HEAD"))
	private void bigglobe_stopGenerator(CallbackInfo callback) {
		VoxyWorldGenerator generator = this.bigglobe_generator;
		if (generator != null) {
			generator.stop();
			generator.save();
		}
	}
}