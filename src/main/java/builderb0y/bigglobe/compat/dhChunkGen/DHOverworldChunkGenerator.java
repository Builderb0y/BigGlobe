package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.util.FullDataPointUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.block.BiomeWrapper;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.block.BlockStateWrapper;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.ChunkSection;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.SkylandSurfaceSettings;
import builderb0y.bigglobe.util.AsyncConsumer;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class DHOverworldChunkGenerator implements IDhApiWorldGenerator {

	public final ILevelWrapper level;
	public final ServerWorld serverWorld;
	public final BigGlobeOverworldChunkGenerator generator;
	public final ChunkSection[] sharedSections;

	public DHOverworldChunkGenerator(ILevelWrapper level, ServerWorld serverWorld, BigGlobeOverworldChunkGenerator generator) {
		this.level = level;
		this.serverWorld = serverWorld;
		this.generator = generator;
		this.sharedSections = new ChunkSection[serverWorld.countVerticalSections()];
		Registry<Biome> biomeRegistry = serverWorld.getRegistryManager().get(RegistryKeyVersions.biome());
		RegistryEntry<Biome> plains = biomeRegistry.entryOf(BiomeKeys.PLAINS);
		for (int index = 0, length = this.sharedSections.length; index < length; index++) {
			this.sharedSections[index] = new ChunkSection(#if MC_VERSION <= MC_1_19_4 index, #endif biomeRegistry);
			this.sharedSections[index].populateBiomes((int x, int y, int z, MultiNoiseSampler noise) -> plains, null, 0, #if MC_VERSION > MC_1_19_4 0, #endif 0);
		}
	}

	@Override
	public boolean isBusy() {
		return false;
	}

	@Override
	public CompletableFuture<Void> generateChunks(
		int chunkPosMinX,
		int chunkPosMinZ,
		byte granularity,
		byte targetDataDetail,
		EDhApiDistantGeneratorMode generatorMode,
		ExecutorService worldGeneratorThreadPool,
		Consumer<Object[]> resultConsumer
	) {
		return CompletableFuture.runAsync(
			() -> {
				int chunkPosMaxX = chunkPosMinX + (1 << (granularity - 4));
				int chunkPosMaxZ = chunkPosMinZ + (1 << (granularity - 4));
				Registry<Biome> biomeRegistry = this.serverWorld.getRegistryManager().get(RegistryKeyVersions.biome());
				RegistryEntry<Biome> plains = biomeRegistry.entryOf(BiomeKeys.PLAINS);
				IBiomeWrapper plainsWrapper = BiomeWrapper.getBiomeWrapper(plains, this.level);
				for (int chunkZ = chunkPosMinZ; chunkZ < chunkPosMaxZ; chunkZ++) {
					for (int chunkX = chunkPosMinX; chunkX < chunkPosMaxX; chunkX++) {
						FakeChunk fakeChunk = new FakeChunk(new ChunkPos(chunkX, chunkZ), this.serverWorld, this.level, plainsWrapper, this.sharedSections);
						fakeChunk.setLightOn(true);
						resultConsumer.accept(new Object[] { fakeChunk, this.serverWorld });
					}
				}
			},
			worldGeneratorThreadPool
		);
	}

	public static @Nullable ChunkSizedFullDataAccessor generateForReal(FakeChunk fakeChunk) {
		record Data(int x, int z, long[] payload) {}
		if (fakeChunk.getServerWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator generator) {
			ChunkSizedFullDataAccessor result = new ChunkSizedFullDataAccessor(new DhChunkPos(fakeChunk.getPos().x, fakeChunk.getPos().z));
			ChunkOfColumns<OverworldColumn> columns = generator.chunkOfColumnsRecycler.get().asType(OverworldColumn.class);
			columns.setPosUncheckedAndPopulate(fakeChunk.getPos().getStartX(), fakeChunk.getPos().getStartZ(), column -> {
				column.getSnowHeight();
				if (column.getFinalTopHeightD() < column.getSeaLevel()) {
					column.getSkylandMinY();
					column.getSkylandMaxY();

					column.getGlacierTopHeightD();
					column.getGlacierCell();
					column.getGlacierCrackFraction();
					column.getGlacierCrackThreshold();
				}
			});
			int stoneID      = result.getMapping().addIfNotPresentAndGetId(fakeChunk.biome, BlockStateWrapper.fromBlockState(BlockStates.STONE, fakeChunk.level));
			int waterID      = result.getMapping().addIfNotPresentAndGetId(fakeChunk.biome, BlockStateWrapper.fromBlockState(BlockStates.WATER, fakeChunk.level));
			int floatstoneID = result.getMapping().addIfNotPresentAndGetId(fakeChunk.biome, BlockStateWrapper.fromBlockState(BlockStates.FLOATSTONE, fakeChunk.level));
			int snowID       = result.getMapping().addIfNotPresentAndGetId(fakeChunk.biome, BlockStateWrapper.fromBlockState(BlockStates.SNOW.with(SnowBlock.LAYERS, SnowBlock.MAX_LAYERS), fakeChunk.level));
			try (AsyncConsumer<Data> async = new AsyncConsumer<>(data -> {
				result.setSingleColumn(data.payload, data.x, data.z);
			})) {
				int seaLevel = columns.getColumn(0).settings.height.sea_level();
				int chunkBottomY = fakeChunk.getBottomY();
				for (int columnIndex = 0; columnIndex < 256; columnIndex++) {
					final int columnIndex_ = columnIndex;
					async.submit(() -> {
						DataPacker packer = new DataPacker(result, fakeChunk.level, fakeChunk.getBottomY(), fakeChunk.getTopY());
						OverworldColumn column = columns.getColumn(columnIndex_);
						Permuter permuter = new Permuter(0L);
						double surfaceD = column.getFinalTopHeightD();
						int surfaceI = BigGlobeMath.ceilI(surfaceD);
						byte lightLevel = 0x0F;
						//skylands
						if (column.hasSkyland()) {
							double skylandMaxY = column.getSkylandMaxY();
							double skylandMinY = column.getSkylandMinY();
							int skylandUnderI = BigGlobeMath.floorI(skylandMinY);
							int skylandSurfaceI = BigGlobeMath.ceilI(skylandMaxY);
							SkylandSurfaceSettings primarySkylandSurface = column.getSkylandCell().settings.surface;
							double skylandDerivativeMagnitudeSquared = BigGlobeMath.squareD(
								BigGlobeOverworldChunkGenerator.estimateSkylandDelta(columns, column::blankCopy, columnIndex_, 1,  skylandMaxY),
								BigGlobeOverworldChunkGenerator.estimateSkylandDelta(columns, column::blankCopy, columnIndex_, 16, skylandMaxY)
							);
							permuter.setSeed(Permuter.permute(fakeChunk.getServerWorld().getSeed() ^ 0x4E3FC19AC72F9842L, column.x, column.z));
							int skylandSurfaceDepth = BigGlobeMath.ceilI(primarySkylandSurface.primary_depth().evaluate(column, skylandMaxY, skylandDerivativeMagnitudeSquared, permuter));
							if (skylandSurfaceDepth > 0) { //skyland has surface.
								BlockState skylandSurfaceState = primarySkylandSurface.primary().top();
								int surfaceBottomY = skylandSurfaceI - skylandSurfaceDepth;
								if (surfaceBottomY > skylandUnderI) { //less surface than floatstone.
									packer.add(packer.id(fakeChunk.biome, packer.state(skylandSurfaceState)), surfaceBottomY, skylandSurfaceI, lightLevel);
									packer.add(floatstoneID, skylandUnderI, surfaceBottomY, lightLevel);
								}
								else { //more or equal surface than floatstone.
									packer.add(packer.id(fakeChunk.biome, packer.state(skylandSurfaceState)), skylandUnderI, skylandSurfaceI, lightLevel);
								}
							}
							else { //no surface, only floatstone.
								packer.add(floatstoneID, skylandUnderI, skylandSurfaceI, lightLevel);
							}
							lightLevel = 0x00;
						}
						if (surfaceI < seaLevel) { //ocean.
							if (column.getGlacierCrackFraction() <= column.getGlacierCrackThreshold()) {
								//glaciers.
								packer.add(snowID, seaLevel, BigGlobeMath.ceilI(column.getGlacierTopHeightD()), lightLevel);
							}
							//water.
							packer.add(waterID, surfaceI, seaLevel, lightLevel);
							lightLevel = (byte)(Math.min(lightLevel, Math.max(15 - (seaLevel - surfaceI), 0)));
						}
						else { //land.
							int snowY = BigGlobeMath.ceilI(column.getSnowHeight());
							if (snowY > surfaceI) {
								//snow.
								packer.add(snowID, surfaceI, snowY, lightLevel);
							}
						}
						{
							double derivativeMagnitudeSquared = BigGlobeMath.squareD(
								columns.getColumn(columnIndex_ ^  1).getFinalTopHeightD() - surfaceD,
								columns.getColumn(columnIndex_ ^ 16).getFinalTopHeightD() - surfaceD
							);
							permuter.setSeed(Permuter.permute(fakeChunk.getServerWorld().getSeed() ^ 0x7EF4E9F5C88A2506L, column.x, column.z));
							int primaryDepth = BigGlobeMath.floorI(column.settings.surface.primary_surface_depth().evaluate(column, surfaceD, derivativeMagnitudeSquared, permuter));
							if (primaryDepth > 0) { //ordinary surface.
								BlockState surfaceState = column.settings.biomes.getPrimarySurface(column, surfaceD, fakeChunk.getServerWorld().getSeed()).top();
								int surfaceID = packer.id(fakeChunk.biome, packer.state(surfaceState));
								packer.add(surfaceID, surfaceI - primaryDepth, surfaceI, lightLevel);
								packer.add(stoneID, chunkBottomY, surfaceI - primaryDepth, lightLevel);
							}
							else { //no surface. just stone.
								packer.add(stoneID, chunkBottomY, surfaceI, lightLevel);
							}
						}
						return new Data(column.x & 15, column.z & 15, packer.pack(0));
					});
				}
			}
			generator.chunkOfColumnsRecycler.reclaim(columns);
			return result;
		}
		return null;
	}

	public static long encodeUnchecked(int id, int depth, int y, byte lightPair) {
		long data = 0;
		data |= id & FullDataPointUtil.ID_MASK;
		data |= (long)(depth & FullDataPointUtil.DP_MASK) << FullDataPointUtil.DP_OFFSET;
		data |= (long)(y & FullDataPointUtil.Y_MASK) << FullDataPointUtil.Y_OFFSET;
		data |= (long)lightPair << FullDataPointUtil.LIGHT_OFFSET;
		return data;
	}

	@Override
	public void preGeneratorTaskStart() {

	}

	@Override
	public void close() {

	}

	@Override
	public byte getMaxGenerationGranularity() {
		return 9;
	}
}