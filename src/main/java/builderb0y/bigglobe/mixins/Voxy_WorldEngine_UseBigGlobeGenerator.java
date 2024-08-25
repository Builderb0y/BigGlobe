package builderb0y.bigglobe.mixins;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.VoxyWorldGenerator2;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixinInterfaces.VoxyGeneratorHolder;

@Mixin(WorldEngine.class)
public class Voxy_WorldEngine_UseBigGlobeGenerator implements VoxyGeneratorHolder {

	public VoxyWorldGenerator2 bigglobe_generator;

	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
	private void bigglobe_startGenerator(
		StorageBackend storageBackend,
		int ingestWorkers,
		int savingServiceWorkers,
		int maxMipLayers,
		CallbackInfo callback
	) {
		MinecraftServer server;
		ServerWorld serverWorld;
		if (
			BigGlobeConfig.INSTANCE.get().voxyIntegration.useWorldgenThread &&
			(server = MinecraftClient.getInstance().getServer()) != null &&
			(serverWorld = server.getWorld(MinecraftClient.getInstance().world.getRegistryKey())) != null &&
			serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator
		) {
			(this.bigglobe_generator = new VoxyWorldGenerator2(generator, (WorldEngine)(Object)(this))).start();
		}
	}

	@Inject(method = "shutdown", at = @At("HEAD"), remap = false)
	private void bigglobe_stopGenerator(CallbackInfo callback) {
		VoxyWorldGenerator2 generator = this.bigglobe_generator;
		if (generator != null) {
			generator.stop();
		}
	}

	@Override
	public VoxyWorldGenerator2 bigglobe_getVoxyGenerator() {
		return this.bigglobe_generator;
	}
}