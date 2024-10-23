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
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.commands.VoxyDebugCommand;
import builderb0y.bigglobe.compat.voxy.QueueingStorageBackend.GenerationQueue;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter;
import builderb0y.bigglobe.util.AsyncConsumer;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
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
		Params params = new Params(generator, 0, 0, ColumnUsage.RAW_GENERATION.voxyHints(0));
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
			if (BigGlobeThreadPool.isBusy()) try {
				Thread.sleep(100L);
			}
			catch (InterruptedException ignored) {}
			if (!this.generateNextChunk()) try {
				Thread.sleep(1000L);
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
		long next = this.queue.nextChunk();
		if (next == -1L) return false;
		this.createChunk(WorldEngine.getX(next), WorldEngine.getZ(next), WorldEngine.getLevel(next));
		return true;
	}

	public abstract void createChunk(int levelX, int levelZ, int level);

	public void convertSection(int levelX, int levelZ, int level, BlockSegmentList[] lists) {
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		boolean lightAir = BigGlobeConfig.INSTANCE.get().voxyIntegration.lightAir;
		try (AsyncRunner async = new AsyncRunner(BigGlobeThreadPool.lodExecutor())) {
			for (int sectionBottomY = minY & -(1 << (level + 5)); sectionBottomY < maxY; sectionBottomY += 1 << (level + 5)) {
				final int sectionBottomY_ = sectionBottomY;
				async.submit(() -> {
					int levelY = sectionBottomY_ >> (level + 5);
					WorldSection section = lightAir ? this.engine.acquire(level, levelX, levelY, levelZ) : null;
					long[] sectionPayload = lightAir ? ((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData() : null;
					if (lightAir) Arrays.fill(sectionPayload, 0L);
					BlockState previousColumnState = null;
					int previousColumnStateID = -1;
					try {
						for (int relativeZ = 0; relativeZ < 32; relativeZ++) {
							for (int relativeX = 0; relativeX < 32; relativeX++) {
								int packedXZ = (relativeZ << 5) | relativeX;
								BlockSegmentList list = lists[packedXZ];
								int segmentIndex = list.getSegmentIndex(sectionBottomY_, false);
								while (segmentIndex < list.size()) {
									LitSegment segment = list.getLit(segmentIndex++);
									if (segment.minY > (sectionBottomY_ | ((1 << (level + 5)) - 1))) break;
									if (lightAir || !segment.value.isAir()) {
										if (section == null) {
											section = this.engine.acquire(level, levelX, levelY, levelZ);
											sectionPayload = ((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData();
											Arrays.fill(sectionPayload, 0L);
										}
										int minRelativeY = Math.max((segment.minY - sectionBottomY_) >> level, 0);
										int maxRelativeY = Math.min((segment.maxY - sectionBottomY_) >> level, 31);
										if (segment.value != previousColumnState) {
											previousColumnState = segment.value;
											previousColumnStateID = previousColumnState.isAir() ? 0 : this.engine.getMapper().getIdForBlockState(previousColumnState);
										}
										byte startLightLevel = segment.lightLevel;
										int diminishment = previousColumnState.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
										int blockLightLevel = previousColumnState.getLuminance() << 4;
										if (startLightLevel == 0 || diminishment == 0) {
											long id = Mapper.composeMappingId((byte)((15 - startLightLevel) | blockLightLevel), previousColumnStateID, this.plainsBiomeId);
											for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
												int index = WorldSection.getIndex(relativeX, relativeY, relativeZ);
												if (previousColumnStateID == 0 && !Mapper.isAir(sectionPayload[index])) continue;
												sectionPayload[index] = id;
											}
										}
										else {
											for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
												int index = WorldSection.getIndex(relativeX, relativeY, relativeZ);
												if (previousColumnStateID == 0 && !Mapper.isAir(sectionPayload[index])) continue;
												int absoluteY = ((relativeY + 1) << level) - 1 + sectionBottomY_;
												int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - absoluteY), 0);
												sectionPayload[index] = Mapper.composeMappingId((byte)((15 - lightLevel) | blockLightLevel), previousColumnStateID, this.plainsBiomeId);
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
				});
			}
		}
	}
}