package builderb0y.bigglobe.compat.voxy;

import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.voxelization.WorldConversionFactory;
import me.cortex.voxy.common.world.WorldEngine;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.RootLayer;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.ClientWorldEvents;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class VoxyWorldGenerator {

	public static VoxyWorldGenerator INSTANCE;
	public static final int WORLD_SIZE_IN_CHUNKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000 >>> 4);

	public final DistanceGraph distanceGraph;
	public final BigGlobeScriptedChunkGenerator generator;
	public final Thread thread;
	public volatile boolean running;

	public VoxyWorldGenerator(BigGlobeScriptedChunkGenerator generator) {
		this.distanceGraph = new DistanceGraph(-WORLD_SIZE_IN_CHUNKS, -WORLD_SIZE_IN_CHUNKS, +WORLD_SIZE_IN_CHUNKS, +WORLD_SIZE_IN_CHUNKS);
		this.generator = generator;
		this.thread = new Thread(this::runLoop, "Big Globe Voxy dispatch thread");
	}

	public static void init() {
		ClientWorldEvents.LOAD.register((ClientWorld world) -> {
			IntegratedServer server = MinecraftClient.getInstance().getServer();
			if (server != null) {
				ServerWorld serverWorld = server.getWorld(world.getRegistryKey());
				if (serverWorld != null && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
					(INSTANCE = new VoxyWorldGenerator(generator)).start();
				}
			}
		});
		ClientWorldEvents.UNLOAD.register(() -> {
			if (INSTANCE != null) {
				INSTANCE.stop();
			}
			INSTANCE = null;
		});
	}

	public void start() {
		this.running = true;
		this.thread.start();
	}

	public void stop() {
		this.running = false;
		try {
			this.thread.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.error("Exception stopping " + this.thread.getName() + ": ", exception);
		}
	}

	public void runLoop() {
		while (true) {
			if (!this.running) {
				BigGlobeMod.LOGGER.info("Big Globe Voxy worldgen thread shutting down due to world closing.");
				return;
			}
			if (!this.generateNextChunk()) {
				BigGlobeMod.LOGGER.info("Big Globe Voxy worldgen thread shutting down because it has finished generating every chunk in the world. How long did that take you?");
				return;
			}
		}
	}

	public boolean generateNextChunk() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return true;
		Reference<Biome> biome = player.getWorld().getRegistryManager().get(RegistryKeyVersions.biome()).entryOf(BiomeKeys.PLAINS);

		int chunkX = player.getBlockX() >> 4;
		int chunkZ = player.getBlockZ() >> 4;
		DistanceGraph.Query query = this.distanceGraph.query(chunkX, chunkZ);
		if (query == null) return false;
		WorldEngine worldEngine = ((IGetVoxelCore)(MinecraftClient.getInstance().worldRenderer)).getVoxelCore().getWorldEngine();
		this.createChunk(query.closestX, query.closestZ, biome, worldEngine);
		return true;
	}

	public void createChunk(int chunkX, int chunkZ, RegistryEntry<Biome> biome, WorldEngine engine) {
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		BlockSegmentList[] lists = new BlockSegmentList[256];
		ScriptedColumn.Factory factory = this.generator.columnEntryRegistry.columnFactory;
		for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
			for (int offsetX = 0; offsetX < 16; offsetX += 2) {
				long seed = this.generator.seed;
				int minY = this.generator.height.min_y();
				int maxY = this.generator.height.max_y();
				RootLayer layer = this.generator.layer;
				int quadX = startX | offsetX;
				int quadZ = startZ | offsetZ;
				ScriptedColumn
					column00 = factory.create(seed, quadX,     quadZ,     minY, maxY, true),
					column01 = factory.create(seed, quadX | 1, quadZ,     minY, maxY, true),
					column10 = factory.create(seed, quadX,     quadZ | 1, minY, maxY, true),
					column11 = factory.create(seed, quadX | 1, quadZ | 1, minY, maxY, true);
				BlockSegmentList
					list00 = new BlockSegmentList(minY, maxY),
					list01 = new BlockSegmentList(minY, maxY),
					list10 = new BlockSegmentList(minY, maxY),
					list11 = new BlockSegmentList(minY, maxY);
				layer.emitSegments(column00, column01, column10, column11, list00);
				layer.emitSegments(column01, column00, column11, column10, list01);
				layer.emitSegments(column10, column11, column00, column01, list10);
				layer.emitSegments(column11, column10, column01, column00, list11);
				int baseIndex = (offsetZ << 4) | offsetX;
				lists[baseIndex     ] = list00;
				lists[baseIndex ^  1] = list01;
				lists[baseIndex ^ 16] = list10;
				lists[baseIndex ^ 17] = list11;
			}
		}
		for (int y = this.generator.height.min_y(), maxY = this.generator.height.max_y(); y < maxY; y += 16) {
			VoxelizedSection section = this.convertSection(chunkX, y >> 4, chunkZ, lists, biome, engine);
			if (section != null) engine.insertUpdate(section);
		}
	}

	public @Nullable VoxelizedSection convertSection(int chunkX, int chunkY, int chunkZ, BlockSegmentList[] lists, RegistryEntry<Biome> biome, WorldEngine engine) {
		long[] section = null;
		for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
			for (int relativeX = 0; relativeX < 16; relativeX++) {
				int packedXZMipped = (relativeZ << 4) | relativeX;
				BlockSegmentList list = lists[(relativeZ << 4) | relativeX];
				int segmentIndex = list.getSegmentIndex(chunkY << 4, false);
				while (segmentIndex < list.size()) {
					Segment<BlockState> segment = list.get(segmentIndex++);
					if (segment.minY > ((chunkY << 4) | 15)) break;
					if (!segment.value.isAir()) {
						if (section == null) section = new long[16 * 16 * 16 + 8 * 8 * 8 + 4 * 4 * 4 + 2 * 2 * 2 + 1];
						int minY = Math.max(segment.minY - (chunkY << 4), 0);
						int maxY = Math.min(segment.maxY - (chunkY << 4), 15);
						long id = engine.getMapper().getBaseId((byte)(0x0F), segment.value, biome);
						for (int relativeY = minY; relativeY <= maxY; relativeY++) {
							section[packedXZMipped | (relativeY << 8)] = id;
						}
					}
				}
			}
		}
		if (section == null) return null;
		VoxelizedSection result = new VoxelizedSection(section, chunkX, chunkY, chunkZ);
		WorldConversionFactory.mipSection(result, engine.getMapper());
		return result;
	}
}