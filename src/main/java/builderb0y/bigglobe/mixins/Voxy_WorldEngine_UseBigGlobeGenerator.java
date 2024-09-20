package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.storage.StorageBackend;
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

	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
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
	}

	@Override
	public AbstractVoxyWorldGenerator bigglobe_getVoxyGenerator() {
		return this.bigglobe_generator;
	}
}