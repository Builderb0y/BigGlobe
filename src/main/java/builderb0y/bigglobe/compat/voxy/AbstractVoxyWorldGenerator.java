package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;
import java.util.Arrays;

import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.world.SaveLoadSystem;
import me.cortex.voxy.common.world.WorldEngine;
import me.cortex.voxy.common.world.WorldSection;
import me.cortex.voxy.common.world.other.Mapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.commands.VoxyDebugCommand;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.mixins.Voxy_WorldEngine_Accessors;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_Accessors;
import builderb0y.bigglobe.mixins.Voxy_WorldSection_DataGetter;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

@Environment(EnvType.CLIENT)
public abstract class AbstractVoxyWorldGenerator {

	public static final int WORLD_SIZE_IN_CHUNKS = MathHelper.smallestEncompassingPowerOfTwo(30_000_000 >>> 4);

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
			if (backend instanceof GeneratingStorageBackend generating) {
				generating.generator = this;
			}
		}
		this.columns = ThreadLocal.withInitial(() -> {
			ScriptedColumn[] columns = new ScriptedColumn[1024];
			ScriptedColumn.Factory factory = generator.columnEntryRegistry.columnFactory;
			Params params = new Params(generator, 0, 0, Purpose.RAW_VOXY);
			for (int index = 0; index < 1024; index++) {
				columns[index] = factory.create(params);
			}
			return columns;
		});
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

	public void createChunk(long position, StorageBackend storage) {
		this.createChunk(
			WorldEngine.getX(position),
			WorldEngine.getZ(position),
			WorldEngine.getLevel(position),
			storage
		);
	}

	public abstract void createChunk(int levelX, int levelZ, int level, StorageBackend storage);

	public void convertSection(int levelX, int levelZ, int level, BlockSegmentList[] lists, StorageBackend storage) {
		int minY = this.generator.height.min_y();
		int maxY = this.generator.height.max_y();
		boolean lightAir = BigGlobeConfig.INSTANCE.get().voxyIntegration.lightAir;
		for (int sectionBottomY = minY & -(1 << (level + 5)); sectionBottomY < maxY; sectionBottomY += 1 << (level + 5)) {
			WorldSection section = lightAir ? Voxy_WorldSection_Accessors.bigglobe_create(level, levelX, sectionBottomY >> (level + 5), levelZ, ((Voxy_WorldEngine_Accessors)(this.engine)).bigglobe_getTracker()) : null;
			long[] sectionPayload = lightAir ? ((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData() : null;
			//if (lightAir) Arrays.fill(sectionPayload, 0L);
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
							if (lightAir || !segment.value.isAir()) {
								if (section == null) {
									section = Voxy_WorldSection_Accessors.bigglobe_create(level, levelX, sectionBottomY >> (level + 5), levelZ, ((Voxy_WorldEngine_Accessors)(this.engine)).bigglobe_getTracker());
									sectionPayload = ((Voxy_WorldSection_DataGetter)(Object)(section)).bigglobe_getData();
									//Arrays.fill(sectionPayload, 0L);
								}
								int minRelativeY = Math.max((segment.minY - sectionBottomY) >> level, 0);
								int maxRelativeY = Math.min((segment.maxY - sectionBottomY) >> level, 31);
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
										int absoluteY = ((relativeY + 1) << level) - 1 + sectionBottomY;
										int lightLevel = Math.max(startLightLevel - diminishment * (segment.maxY - absoluteY), 0);
										sectionPayload[index] = Mapper.composeMappingId((byte)((15 - lightLevel) | blockLightLevel), previousColumnStateID, this.plainsBiomeId);
									}
								}
							}
						}
					}
				}
				if (section != null) {
					ByteBuffer data = SaveLoadSystem.serialize(section);
					try {
						storage.setSectionData(section.key, data);
					}
					finally {
						MemoryUtil.memFree(data);
					}
				}
			}
			finally {
				if (section != null) ((Voxy_WorldSection_Accessors)(Object)(section)).bigglobe_trySetFreed();
			}
		}
	}
}