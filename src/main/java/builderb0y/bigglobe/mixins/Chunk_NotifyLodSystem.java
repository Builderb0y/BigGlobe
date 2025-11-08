package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.ImmersivePortalsCompat;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Mixin(Chunk.class)
public abstract class Chunk_NotifyLodSystem {

	@Shadow public abstract ChunkStatus getStatus();

	@Shadow public abstract ChunkPos getPos();

	#if MC_VERSION >= MC_1_21_2

		@Inject(method = "tryMarkSaved", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;needsSaving:Z", opcode = Opcodes.PUTFIELD))
		private void bigglobe_notifyLodSystem(CallbackInfoReturnable<Boolean> callback) {
			this.bigglobe_tryNotify();
		}

	#else

		@Inject(method = "setNeedsSaving", at = @At("HEAD"))
		private void bigglobe_notifyLodSystem(boolean needsSaving, CallbackInfo callback) {
			if (!needsSaving) this.bigglobe_tryNotify();
		}

	#endif

	@Unique
	private void bigglobe_tryNotify() {
		try {
			if (
				this.getStatus() == ChunkStatus.FULL &&
				((Object)(this)) instanceof WorldChunk worldChunk &&
				worldChunk.getWorld() instanceof ServerWorld serverWorld &&
				FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
			) {
				bigglobe_doNotify(serverWorld, this.getPos());
			}
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Exception notifying LOD system of chunk save:", throwable);
		}
	}

	@Unique
	@Environment(EnvType.CLIENT)
	private static void bigglobe_doNotify(ServerWorld world, ChunkPos chunkPos) {
		LodSystemHolder holder = ImmersivePortalsCompat.getLodSystem(world.getRegistryKey());
		if (holder != null) {
			LodSystem system = holder.bigglobe_getLodSystem();
			if (system != null) system.invalidateChunkLater(chunkPos);
		}
	}
}