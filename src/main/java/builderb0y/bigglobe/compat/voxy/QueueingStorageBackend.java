package builderb0y.bigglobe.compat.voxy;

import me.cortex.voxy.common.world.WorldEngine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import builderb0y.bigglobe.compat.voxy.DistanceGraph.Query;

public interface QueueingStorageBackend {

	public abstract GenerationQueue getQueue();

	public abstract void setQueue(GenerationQueue queue);

	public static class GenerationQueue {

		public static final int WORLD_SIZE_IN_LODS = DistanceGraph.WORLD_SIZE_IN_BLOCKS >> 5;

		public final DistanceGraph[]
			graphs = new DistanceGraph[16],
			done   = new DistanceGraph[16];

		public GenerationQueue() {
			for (int lod = 0; lod < 16; lod++) {
				this.graphs[lod] = new DistanceGraph(
					-WORLD_SIZE_IN_LODS >> lod,
					-WORLD_SIZE_IN_LODS >> lod,
					+WORLD_SIZE_IN_LODS >> lod,
					+WORLD_SIZE_IN_LODS >> lod,
					false
				);
				this.done[lod] = new DistanceGraph(
					-WORLD_SIZE_IN_LODS >> lod,
					-WORLD_SIZE_IN_LODS >> lod,
					+WORLD_SIZE_IN_LODS >> lod,
					+WORLD_SIZE_IN_LODS >> lod,
					false
				);
			}
		}

		public synchronized void queueChunk(long key) {
			if (!this.done[WorldEngine.getLevel(key)].get(WorldEngine.getX(key), WorldEngine.getZ(key))) {
				this.graphs[WorldEngine.getLevel(key)].set(WorldEngine.getX(key), WorldEngine.getZ(key), true);
			}
		}

		public synchronized void clearChunk(long key) {
			this.done[WorldEngine.getLevel(key)].set(WorldEngine.getX(key), WorldEngine.getZ(key), false);
		}

		public synchronized long nextChunk() {
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player == null) return -1L;
			int x = player.getBlockX() >> 5;
			int z = player.getBlockZ() >> 5;
			for (int lod = 0; lod < 16; lod++) {
				Query query = this.graphs[lod].next(x >> lod, z >> lod, true);
				if (query != null) {
					this.done[lod].set(query.closestX, query.closestZ, true);
					return WorldEngine.getWorldSectionId(lod, query.closestX, 0, query.closestZ);
				}
			}
			return -1;
		}
	}
}