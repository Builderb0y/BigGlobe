package builderb0y.bigglobe.hyperspace;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public record WorldChunkPos(RegistryKey<World> world, int chunkX, int chunkZ) {

	public WorldChunkPos(RegistryKey<World> world, ChunkPos chunkPos) {
		this(world, chunkPos.x, chunkPos.z);
	}

	public WorldChunkPos(World world, ChunkPos chunkPos) {
		this(world.getRegistryKey(), chunkPos.x, chunkPos.z);
	}

	public WorldChunkPos(WorldChunk chunk) {
		this(chunk.getWorld(), chunk.getPos());
	}
}