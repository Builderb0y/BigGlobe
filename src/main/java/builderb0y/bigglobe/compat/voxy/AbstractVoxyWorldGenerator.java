package builderb0y.bigglobe.compat.voxy;

import java.util.Arrays;

import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.voxelization.VoxelizedSection;
import me.cortex.voxy.common.voxelization.WorldConversionFactory;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.other.Mapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.commands.VoxyDebugCommand;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

@Environment(EnvType.CLIENT)
public abstract class AbstractVoxyWorldGenerator {

	public static final int WORLD_SIZE_IN_CHUNKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000 >>> 4);

	/** can be set by {@link VoxyDebugCommand}. */
	public static @Nullable Factory override;

	public final WorldEngine engine;
	public final ServerWorld world;
	public final BigGlobeScriptedChunkGenerator generator;
	public final DistanceGraph distanceGraph;
	public final Thread thread;
	public final ScriptedColumn[] columns;
	public final long[] sectionInstance;
	public volatile boolean running;

	public AbstractVoxyWorldGenerator(
		WorldEngine engine,
		ServerWorld world,
		BigGlobeScriptedChunkGenerator generator,
		DistanceGraph graph
	) {
		this.engine = engine;
		this.world = world;
		this.distanceGraph = graph;
		this.generator = generator;
		this.thread = new Thread(this::runLoop, "Big Globe Voxy worldgen thread");

		this.columns = new ScriptedColumn[256];
		ScriptedColumn.Factory factory = generator.columnEntryRegistry.columnFactory;
		Params params = new Params(generator, 0, 0, Purpose.RAW_VOXY);
		for (int index = 0; index < 256; index++) {
			this.columns[index] = factory.create(params);
		}
		this.sectionInstance = new long[16 * 16 * 16 + 8 * 8 * 8 + 4 * 4 * 4 + 2 * 2 * 2 + 1];
	}

	public static void reloadWith(Factory factory, IGetVoxelCore coreGetter) {
		AbstractVoxyWorldGenerator.override = factory;
		try {
			coreGetter.reloadVoxelCore();
		}
		finally {
			AbstractVoxyWorldGenerator.override = null;
		}
	}

	public static interface Factory {

		public abstract AbstractVoxyWorldGenerator create(
			WorldEngine engine,
			ServerWorld world,
			BigGlobeScriptedChunkGenerator generator
		);
	}

	public static @Nullable AbstractVoxyWorldGenerator createGenerator(ClientWorld newWorld, WorldEngine engine) {
		MinecraftServer server;
		ServerWorld serverWorld;
		if (
			BigGlobeConfig.INSTANCE.get().voxyIntegration.useWorldgenThread &&
			(server = MinecraftClient.getInstance().getServer()) != null &&
			(serverWorld = server.getWorld(newWorld.getRegistryKey())) != null &&
			serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator
		) {
			Factory factory = override;
			if (factory != null) return factory.create(engine, serverWorld, generator);
			else return new VoxyWorldGenerator(engine, serverWorld, generator);
		}
		else {
			return null;
		}
	}

	public void start() {
		this.running = true;
		this.thread.start();
	}

	public void stop() {
		this.running = false;
		this.thread.interrupt(); //just in case the thread is waiting on error.
		try {
			this.thread.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.error("Exception stopping " + this.thread.getName() + ": ", exception);
		}
	}

	public void runLoop() {
		BigGlobeMod.LOGGER.info("Big Globe voxy generation thread started.");
		int failures = 0;
		while (true) try {
			if (!this.running) {
				BigGlobeMod.LOGGER.info("Big Globe Voxy worldgen thread shutting down due to world closing.");
				break;
			}
			if (!this.generateNextChunk()) {
				BigGlobeMod.LOGGER.info("Big Globe Voxy worldgen thread shutting down because it has finished generating every chunk in the world. How long did that take you?");
				break;
			}
			failures = 0;
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Exception on Big Globe Voxy thread: ", exception);
			if (++failures >= 3) {
				BigGlobeMod.LOGGER.error("Failed 3 times. Assuming state is corrupt or something and shutting down.");
				break;
			}
			else try {
				Thread.sleep(5000L);
			}
			catch (InterruptedException ignored) {}
		}
		this.save();
	}

	public abstract void save();

	public boolean generateNextChunk() {
		if (BigGlobeThreadPool.isBusy()) {
			try {
				Thread.sleep(100L);
			}
			catch (InterruptedException ignored) {}
			return true;
		}
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return true;
		RegistryEntry<Biome> biome = player.getWorld().getRegistryManager().get(RegistryKeyVersions.biome()).entryOf(BiomeKeys.PLAINS);

		int chunkX = player.getBlockX() >> 4;
		int chunkZ = player.getBlockZ() >> 4;
		DistanceGraph.Query query = this.distanceGraph.query(chunkX, chunkZ);
		if (query == null) return false;
		this.createChunk(query.closestX, query.closestZ, biome);
		return true;
	}

	public abstract void createChunk(int chunkX, int chunkZ, RegistryEntry<Biome> biome);

	public @Nullable VoxelizedSection convertSection(int chunkX, int chunkY, int chunkZ, BlockSegmentList[] lists, RegistryEntry<Biome> biome) {
		int biomeID = this.engine.getMapper().getIdForBiome(biome);
		long[] section = null;
		BlockState previousColumnState = null;
		int previousColumnStateID = -1;
		for (int relativeZ = 0; relativeZ < 16; relativeZ++) {
			for (int relativeX = 0; relativeX < 16; relativeX++) {
				int packedXZ = (relativeZ << 4) | relativeX;
				BlockSegmentList list = lists[packedXZ];
				int segmentIndex = list.getSegmentIndex(chunkY << 4, false);
				while (segmentIndex < list.size()) {
					LitSegment segment = list.getLit(segmentIndex++);
					if (segment.minY > ((chunkY << 4) | 15)) break;
					if (!segment.value.isAir()) {
						if (section == null) {
							section = this.sectionInstance;
							Arrays.fill(section, 0L);
						}
						int minY = Math.max(segment.minY - (chunkY << 4), 0);
						int maxY = Math.min(segment.maxY - (chunkY << 4), 15);
						if (segment.value != previousColumnState) {
							previousColumnStateID = this.engine.getMapper().getIdForBlockState(previousColumnState = segment.value);
						}
						byte startLightLevel = segment.lightLevel;
						int diminishment = segment.value.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
						if (startLightLevel == 0 || diminishment == 0) {
							long id = Mapper.composeMappingId((byte)(15 - startLightLevel), previousColumnStateID, biomeID);
							for (int relativeY = minY; relativeY <= maxY; relativeY++) {
								section[packedXZ | (relativeY << 8)] = id;
							}
						}
						else {
							for (int relativeY = minY; relativeY <= maxY; relativeY++) {
								int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - (relativeY + (chunkY << 4))), 0);
								section[packedXZ | (relativeY << 8)] = Mapper.composeMappingId((byte)(15 - lightLevel), previousColumnStateID, biomeID);
							}
						}
					}
				}
			}
		}
		if (section == null) return null;
		VoxelizedSection result = new VoxelizedSection(section, chunkX, chunkY, chunkZ);
		WorldConversionFactory.mipSection(result, this.engine.getMapper());
		return result;
	}
}