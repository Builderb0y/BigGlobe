package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunk.EntityLoader;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.mixinInterfaces.ChunkOfColumnsHolder;

/**
when a {@link ProtoChunk} is converted into a full {@link WorldChunk},
we want to recycle all the columns it held, since ProtoChunk implements
{@link ChunkOfColumnsHolder} via {@link ProtoChunk_ImplementChunkOfColumnsHolder}.
*/
@Mixin(WorldChunk.class)
public class WorldChunk_RecycleColumnsOnConstruct {

	@Inject(method = "<init>(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/ProtoChunk;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;)V", at = @At("RETURN"))
	private void bigglobe_recycleColumns(ServerWorld world, ProtoChunk protoChunk, EntityLoader entityLoader, CallbackInfo callback) {
		if (protoChunk instanceof ChunkOfColumnsHolder columnsHolder && columnsHolder.bigglobe_getChunkOfColumns() != null && world.getChunkManager().getChunkGenerator() instanceof BigGlobeChunkGenerator generator) {
			generator.chunkOfColumnsRecycler.reclaim(columnsHolder.bigglobe_getChunkOfColumns());
			columnsHolder.bigglobe_setChunkOfColumns(null);
		}
	}
}