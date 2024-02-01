package builderb0y.bigglobe.compat.dhChunkGen;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.SkylandSurfaceSettings;

public class DhOverworldWorldGenerator extends AbstractDhWorldGenerator {

	public final BigGlobeOverworldChunkGenerator generator;

	public DhOverworldWorldGenerator(IDhApiLevelWrapper level, ServerWorld serverWorld, BigGlobeOverworldChunkGenerator generator) {
		super(level, serverWorld);
		this.generator = generator;
	}

	@Override
	public BigGlobeChunkGenerator getGenerator() {
		return this.generator;
	}

	@Override
	public void prepareColumn(WorldColumn column) {
		OverworldColumn overworldColumn = (OverworldColumn)(column);
		overworldColumn.getSnowHeight();
		overworldColumn.getSkylandMinY();
		overworldColumn.getSkylandMaxY();

		if (overworldColumn.getFinalTopHeightD() < overworldColumn.getSeaLevel()) {
			overworldColumn.getGlacierTopHeightD();
			overworldColumn.getGlacierCell();
			overworldColumn.getGlacierCrackFraction();
			overworldColumn.getGlacierCrackThreshold();
		}
	}

	@Override
	public DataPointListPopulator getDataPointPopulator(int chunkX, int chunkZ) {
		Object[] globalQuery = new Object[1];
		IDhApiBlockStateWrapper      stoneWrapper = this.blockState(globalQuery, BlockStates.STONE);
		IDhApiBlockStateWrapper      waterWrapper = this.blockState(globalQuery, BlockStates.WATER);
		IDhApiBlockStateWrapper floatstoneWrapper = this.blockState(globalQuery, BlockStates.FLOATSTONE);
		IDhApiBlockStateWrapper       snowWrapper = this.blockState(globalQuery, BlockStates.SNOW);
		int seaLevel = this.generator.settings.height.sea_level();

		return (ChunkOfColumns<? extends WorldColumn> columns, int columnIndex, DataPointListBuilder builder) -> {
			OverworldColumn column = (OverworldColumn)(columns.getColumn(columnIndex));
			Permuter permuter = new Permuter(0L);
			double surfaceD = column.getFinalTopHeightD();
			int surfaceI = BigGlobeMath.ceilI(surfaceD);
			//skylands
			if (column.hasSkyland()) {
				double skylandMaxY = column.getSkylandMaxY();
				double skylandMinY = column.getSkylandMinY();
				int skylandUnderI = BigGlobeMath.floorI(skylandMinY);
				int skylandSurfaceI = BigGlobeMath.ceilI(skylandMaxY);
				SkylandSurfaceSettings primarySkylandSurface = column.getSkylandCell().settings.surface;
				double skylandDerivativeMagnitudeSquared = BigGlobeMath.squareD(
					BigGlobeOverworldChunkGenerator.estimateSkylandDelta(columns.asType(OverworldColumn.class), column::blankCopy, columnIndex, 1,  skylandMaxY),
					BigGlobeOverworldChunkGenerator.estimateSkylandDelta(columns.asType(OverworldColumn.class), column::blankCopy, columnIndex, 16, skylandMaxY)
				);
				permuter.setSeed(Permuter.permute(this.serverWorld.getSeed() ^ 0x4E3FC19AC72F9842L, column.x, column.z));
				int skylandSurfaceDepth = BigGlobeMath.ceilI(primarySkylandSurface.primary_depth().evaluate(column, skylandMaxY, skylandDerivativeMagnitudeSquared, permuter));
				if (skylandSurfaceDepth > 0) { //skyland has surface.
					BlockState skylandSurfaceState = primarySkylandSurface.primary().top();
					int surfaceBottomY = skylandSurfaceI - skylandSurfaceDepth;
					if (surfaceBottomY > skylandUnderI) { //less surface than floatstone.
						builder.add(skylandSurfaceState, surfaceBottomY, skylandSurfaceI);
						builder.add(floatstoneWrapper, skylandUnderI, surfaceBottomY);
					}
					else { //more or equal surface than floatstone.
						builder.add(skylandSurfaceState, skylandUnderI, skylandSurfaceI);
					}
				}
				else { //no surface, only floatstone.
					builder.add(floatstoneWrapper, skylandUnderI, skylandSurfaceI);
				}
				builder.lightLevel = 0x00;
			}
			if (surfaceI < seaLevel) { //ocean.
				if (column.getGlacierCrackFraction() <= column.getGlacierCrackThreshold()) {
					//glaciers.
					builder.add(snowWrapper, seaLevel, BigGlobeMath.ceilI(column.getGlacierTopHeightD()));
				}
				//water.
				builder.add(waterWrapper, surfaceI, seaLevel);
				builder.lightLevel = Math.min(builder.lightLevel, Math.max(15 - (seaLevel - surfaceI), 0));
			}
			else { //land.
				int snowY = BigGlobeMath.ceilI(column.getSnowHeight());
				if (snowY > surfaceI) {
					//snow.
					builder.add(snowWrapper, surfaceI, snowY);
				}
			}
			{
				double derivativeMagnitudeSquared = BigGlobeMath.squareD(
					columns.getColumn(columnIndex ^  1).getFinalTopHeightD() - surfaceD,
					columns.getColumn(columnIndex ^ 16).getFinalTopHeightD() - surfaceD
				);
				permuter.setSeed(Permuter.permute(this.serverWorld.getSeed() ^ 0x7EF4E9F5C88A2506L, column.x, column.z));
				int primaryDepth = BigGlobeMath.floorI(column.settings.surface.primary_surface_depth().evaluate(column, surfaceD, derivativeMagnitudeSquared, permuter));
				if (primaryDepth > 0) { //ordinary surface.
					//BlockState surfaceState = column.settings.biomes.getPrimarySurface(column, surfaceD, this.serverWorld.getSeed()).top();
					//builder.add(surfaceState, surfaceI - primaryDepth, surfaceI);
					//builder.add(stoneWrapper, column.settings.height.min_y(), surfaceI - primaryDepth);
				}
				else { //no surface. just stone.
					//builder.add(stoneWrapper, column.settings.height.min_y(), surfaceI);
				}
			}
		};
	}
}