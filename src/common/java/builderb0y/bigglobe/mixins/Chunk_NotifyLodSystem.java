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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(ChunkAccess.class)
public abstract class Chunk_NotifyLodSystem {

	@Shadow
	public abstract ChunkStatus getPersistedStatus();

	@Shadow
	public abstract ChunkPos getPos();

	@Inject(method = "tryMarkSaved", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;unsaved:Z", opcode = Opcodes.PUTFIELD))
	private void bigglobe_notifyLodSystem(CallbackInfoReturnable<Boolean> callback) {
		try {
			if (
				this.getPersistedStatus() == ChunkStatus.FULL &&
				((Object)(this)) instanceof LevelChunk worldChunk &&
				worldChunk.getLevel() instanceof ServerLevel serverWorld &&
				FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
			) {
				/*
				//todo: re-enable once rendering is re-written.
				LodSystemHolder holder = ImmersivePortalsCompat.getLodSystem(world.dimension());
				if (holder != null) {
					LodSystem system = holder.bigglobe_getLodSystem();
					if (system != null) system.invalidateChunkLater(chunkPos);
				}
				*/
			}
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Exception notifying LOD system of chunk save:", throwable);
		}
	}
}