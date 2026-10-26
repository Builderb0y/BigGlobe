package builderb0y.bigglobe.mixins;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.compat.ImmersivePortalsCompat;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.rendering.lods.LodSystem;

@Mixin(ChunkAccess.class)
public abstract class Chunk_NotifyLodSystem {

	@Shadow
	public abstract ChunkStatus getPersistedStatus();

	@Shadow
	public abstract ChunkPos getPos();

	@Shadow
	public abstract boolean isUnsaved();

	@Inject(method = "setUnsaved", at = @At("HEAD"))
	private void bigglobe_notifyLodSystem(boolean unsaved, CallbackInfo callback) {
		if (!unsaved && this.isUnsaved()) {
			try {
				if (
					this.getPersistedStatus() == ChunkStatus.FULL &&
					((Object)(this)) instanceof LevelChunk worldChunk &&
					worldChunk.getLevel() instanceof ServerLevel serverWorld &&
					FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
				) {
					bigglobe_doNotify(serverWorld, this.getPos());
				}
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception notifying LOD system of chunk save:", throwable);
			}
		}
	}

	@Unique
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private static void bigglobe_doNotify(ServerLevel world, ChunkPos chunkPos) {
		LodSystemHolder holder = ImmersivePortalsCompat.getLodSystem(world.dimension());
		if (holder != null) {
			LodSystem system = holder.bigglobe_getLodSystem();
			if (system != null) system.invalidateChunkLater(chunkPos);
		}
	}
}
