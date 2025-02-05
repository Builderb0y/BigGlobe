package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.cortex.voxy.client.saver.ContextSelectionSystem;
import me.cortex.voxy.common.config.section.SectionSerializationStorage;
import me.cortex.voxy.common.config.section.SectionStorage;
import me.cortex.voxy.common.config.storage.inmemory.MemoryStorageBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.GeneratingStorageBackend;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(value = ContextSelectionSystem.Selection.class, remap = false)
public class Voxy_ContextSelectionSystem_UseMemoryStorageBackendForDebugging {

	@Inject(method = "createSectionStorageBackend", at = @At("HEAD"), cancellable = true)
	private void bigglobe_useForgetfulStorageBackendWhenDebugging(CallbackInfoReturnable<SectionStorage> callback) {
		if (AbstractVoxyWorldGenerator.override != null) {
			callback.setReturnValue(new GeneratingStorageBackend(new SectionSerializationStorage(new MemoryStorageBackend())));
		}
	}

	@ModifyReturnValue(method = "createSectionStorageBackend", at = @At("RETURN"))
	private SectionStorage bigglobe_useGeneratingStorageBackend(SectionStorage original) {
		ClientWorld clientWorld;
		ServerWorld serverWorld;
		MinecraftServer server;
		if (
			BigGlobeConfig.INSTANCE.get().voxyIntegration.useWorldgenThread &&
			(clientWorld = MinecraftClient.getInstance().world) != null &&
			(server = MinecraftClient.getInstance().getServer()) != null &&
			(serverWorld = server.getWorld(clientWorld.getRegistryKey())) != null &&
			serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator
		) {
			return new GeneratingStorageBackend(original);
		}
		else {
			return original;
		}
	}
}