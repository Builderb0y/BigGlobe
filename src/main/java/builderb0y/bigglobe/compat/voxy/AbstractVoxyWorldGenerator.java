package builderb0y.bigglobe.compat.voxy;

import java.util.Arrays;

import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.commands.VoxyDebugCommand;
import builderb0y.bigglobe.compat.voxy.QueueingStorageBackend.GenerationQueue;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

@Environment(EnvType.CLIENT)
public abstract class AbstractVoxyWorldGenerator {

	public static final int WORLD_SIZE_IN_CHUNKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000 >>> 4);

	/** can be set by {@link VoxyDebugCommand}. */
	public static @Nullable Factory override;

	public final WorldEngine engine;
	public final BigGlobeScriptedChunkGenerator generator;
	public final GenerationQueue queue;
	public final Thread thread;
	public final ScriptedColumn[] columns;
	public volatile boolean running;
	public final int plainsBiomeId;

	public AbstractVoxyWorldGenerator(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator generator) {
		this.engine = engine;
		this.generator = generator;
		this.queue = new GenerationQueue();
		for (StorageBackend backend : engine.storage.collectAllBackends()) {
			if (backend instanceof QueueingStorageBackend queueing) {
				queueing.setQueue(this.queue);
			}
		}
		this.thread = new Thread(this::runLoop, "Big Globe Voxy worldgen thread");
		this.columns = new ScriptedColumn[1024];
		ScriptedColumn.Factory factory = generator.columnEntryRegistry.columnFactory;
		Params params = new Params(generator, 0, 0, Purpose.RAW_VOXY);
		for (int index = 0; index < 1024; index++) {
			this.columns[index] = factory.create(params);
		}
		this.plainsBiomeId = engine.getMapper().getIdForBiome(world.getRegistryManager().get(RegistryKeyVersions.biome()).entryOf(BiomeKeys.PLAINS));
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
			ServerWorld serverWorld,
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
		try {
			this.thread.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.error("Unexpected interrupt while stopping " + this.thread.getName() + ": ", exception);
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
			if (!this.generateNextChunk()) try {
				Thread.sleep(500L);
			}
			catch (InterruptedException ignored) {}
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
	}

	public boolean generateNextChunk() {
		long next = this.queue.poll();
		if (next == -1L) return false;
		this.createChunk(WorldEngine.getX(next), WorldEngine.getZ(next), WorldEngine.getLevel(next));
		return true;
	}

	public abstract void createChunk(int levelX, int levelZ, int level);

	public void convertSection(int levelX, int levelZ, int level, BlockSegmentList[] lists) {
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		for (int sectionBottomY = minY & -(1 << (level + 5)); sectionBottomY < maxY; sectionBottomY += 1 << (level + 5)) {
			WorldSection section = null;
			BlockState previousColumnState = null;
			int previousColumnStateID = -1;
			try {
				for (int relativeZ = 0; relativeZ < 32; relativeZ++) {
					for (int relativeX = 0; relativeX < 32; relativeX++) {
						int packedXZ = (relativeZ << 5) | relativeX;
						BlockSegmentList list = lists[packedXZ];
						int segmentIndex = list.getSegmentIndex(sectionBottomY, false);
						while (segmentIndex < list.size()) {
							LitSegment segment = list.getLit(segmentIndex++);
							if (segment.minY > (sectionBottomY | ((1 << (level + 5)) - 1))) break;
							if (!segment.value.isAir()) {
								if (section == null) {
									section = this.engine.acquire(level, levelX, sectionBottomY >> (level + 5), levelZ);
									Arrays.fill(((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData(), 0L);
								}
								int minRelativeY = Math.max((segment.minY - sectionBottomY) >> level, 0);
								int maxRelativeY = Math.min((segment.maxY - sectionBottomY) >> level, 31);
								if (segment.value != previousColumnState) {
									previousColumnStateID = this.engine.getMapper().getIdForBlockState(previousColumnState = segment.value);
								}
								byte startLightLevel = segment.lightLevel;
								int diminishment = previousColumnState.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
								if (startLightLevel == 0 || diminishment == 0) {
									long id = Mapper.composeMappingId((byte)(15 - startLightLevel), previousColumnStateID, this.plainsBiomeId);
									for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
										section.set(relativeX, relativeY, relativeZ, id);
									}
								}
								else {
									for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
										int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - (relativeY + sectionBottomY)), 0);
										section.set(relativeX, relativeY, relativeZ, Mapper.composeMappingId((byte)(15 - lightLevel), previousColumnStateID, this.plainsBiomeId));
									}
								}
							}
						}
					}
				}
				if (section != null) this.engine.markDirty(section);
			}
			finally {
				if (section != null) section.release();
			}
		}
	}
}