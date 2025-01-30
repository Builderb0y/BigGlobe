package builderb0y.bigglobe.compat.voxy;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.util.MemoryBuffer;
import me.cortex.voxy.common.util.VolatileHolder;
import me.cortex.voxy.common.world.SaveLoadSystem;
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
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.commands.VoxyDebugCommand;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_ConstructorAccess;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

@Environment(EnvType.CLIENT)
public abstract class AbstractVoxyWorldGenerator {

	/** can be set by {@link VoxyDebugCommand}. */
	public static @Nullable Factory override;

	public final WorldEngine engine;
	public final BigGlobeScriptedChunkGenerator generator;
	public final ThreadLocal<ScriptedColumn[]> columns;
	public final int plainsBiomeId;

	public AbstractVoxyWorldGenerator(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator generator) {
		this.engine = engine;
		this.generator = generator;
		for (StorageBackend backend : engine.storage.collectAllBackends()) {
			if (backend instanceof QueueingStorageBackend queueing) {
				queueing.setGenerator(this);
			}
		}
		ScriptedColumn.Factory factory = generator.columnEntryRegistry.columnFactory;
		Params params = new Params(generator, 0, 0, ColumnUsage.RAW_GENERATION.voxyHints(0));
		this.columns = ThreadLocal.withInitial(() -> {
			ScriptedColumn[] columns = new ScriptedColumn[1024];
			for (int index = 0; index < 1024; index++) {
				columns[index] = factory.create(params);
			}
			return columns;
		});
		this.plainsBiomeId = engine.getMapper().getIdForBiome(RegistryVersions.getEntry(world.getRegistryManager(), BiomeKeys.PLAINS));
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

	public MemoryBuffer generateNextChunk(long key) {
		return this.createChunk(key, WorldEngine.getX(key), WorldEngine.getZ(key), WorldEngine.getLevel(key));
	}

	public abstract MemoryBuffer createChunk(long key, int levelX, int levelZ, int level);

	public static final VarHandle nonEmptyChildHandle, nonEmptyBlockHandle;
	static {
		try {
			Field field;

			field = WorldSection.class.getDeclaredField("NON_EMPTY_CHILD_HANDLE");
			field.setAccessible(true);
			nonEmptyChildHandle = (VarHandle)(field.get(null));

			field = WorldSection.class.getDeclaredField("NON_EMPTY_BLOCK_HANDLE");
			field.setAccessible(true);
			nonEmptyBlockHandle = (VarHandle)(field.get(null));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public MemoryBuffer convertSection(long key, int levelX, int levelZ, int level, BlockSegmentList[] lists) {
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		boolean lightAir = true; //required on hierarchical rewrite.
		VolatileHolder<MemoryBuffer> returnValue = new VolatileHolder<>();
		try (AsyncRunner async = new AsyncRunner(BigGlobeThreadPool.lodExecutor())) {
			for (int sectionBottomY = minY & -(1 << (level + 5)); sectionBottomY < maxY; sectionBottomY += 1 << (level + 5)) {
				final int sectionBottomY_ = sectionBottomY;
				async.submit(() -> {
					int levelY = sectionBottomY_ >> (level + 5);
					WorldSection section = lightAir ? Voxy_WorldSection_ConstructorAccess.bigglobe_construct(level, levelX, levelY, levelZ, null) : null;
					long[] sectionPayload = lightAir ? ((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData() : null;
					if (lightAir) {
						section.acquire();
						Arrays.fill(sectionPayload, 0L);
					}
					BlockState previousColumnState = null;
					int previousColumnStateID = -1;
					try {
						int nonEmptyBlocks = 0;
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
											section = Voxy_WorldSection_ConstructorAccess.bigglobe_construct(level, levelX, levelY, levelZ, null);
											section.acquire();
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
										int diminishment = BlockStateVersions.getOpacity(previousColumnState, EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
										int blockLightLevel = previousColumnState.getLuminance() << 4;
										if (startLightLevel == 0 || diminishment == 0) {
											long id = Mapper.composeMappingId((byte)((15 - startLightLevel) | blockLightLevel), previousColumnStateID, this.plainsBiomeId);
											for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
												int index = WorldSection.getIndex(relativeX, relativeY, relativeZ);
												boolean wasAir = Mapper.isAir(sectionPayload[index]);
												if (previousColumnStateID == 0 && !wasAir) continue;
												sectionPayload[index] = id;
												if (wasAir) nonEmptyBlocks++;
											}
										}
										else {
											for (int relativeY = minRelativeY; relativeY <= maxRelativeY; relativeY++) {
												int index = WorldSection.getIndex(relativeX, relativeY, relativeZ);
												boolean wasAir = Mapper.isAir(sectionPayload[index]);
												if (previousColumnStateID == 0 && !wasAir) continue;
												int absoluteY = ((relativeY + 1) << level) - 1 + sectionBottomY_;
												int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - absoluteY), 0);
												sectionPayload[index] = Mapper.composeMappingId((byte)((15 - lightLevel) | blockLightLevel), previousColumnStateID, this.plainsBiomeId);
												if (wasAir) nonEmptyBlocks++;
											}
										}
									}
								}
							}
						}
						if (section != null) {
							nonEmptyChildHandle.setVolatile(section, (byte)(-1));
							nonEmptyBlockHandle.setVolatile(section, nonEmptyBlocks);
							MemoryBuffer data = SaveLoadSystem.serialize(section);
							this.engine.storage.setSectionData(section.key, data);
							if (section.key == key) {
								returnValue.obj = data;
							}
							else {
								data.free();
							}
						}
					}
					finally {
						if (section != null) section.release();
					}
				});
			}
		}
		return returnValue.obj;
	}
}