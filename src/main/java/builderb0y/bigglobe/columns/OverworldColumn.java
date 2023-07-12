package builderb0y.bigglobe.columns;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.noise.ScriptedGrid;
import builderb0y.bigglobe.settings.*;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalCavernSettings;
import builderb0y.bigglobe.settings.OverworldHeightSettings.OverworldCliffSettings;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;
import builderb0y.bigglobe.structures.LakeStructure;

public class OverworldColumn extends WorldColumn {

	public static final int
		HILLINESS                    = 1 << 0,
		CLIFFINESS                   = 1 << 1,
		RAW_EROSION_AND_SNOW         = 1 << 2,
		POST_CLIFF_HEIGHT            = 1 << 3,
		FINAL_HEIGHT                 = 1 << 4,
		TEMPERATURE                  = 1 << 5,
		FOLIAGE                      = 1 << 6,
		SNOW_HEIGHT                  = 1 << 7,
		SURFACE_BIOME                = 1 << 8,

		CAVE_CELL                    = 1 << 9,
		CAVE_NOISE                   = 1 << 10,
		CAVE_SURFACE_DEPTH           = 1 << 11,
		CAVE_SYSTEM_EDGINESS         = 1 << 12,
		CAVE_SYSTEM_EDGINESS_SQUARED = 1 << 13,

		CAVERN_CELL                  = 1 << 14,
		CAVERN_CENTER                = 1 << 15,
		CAVERN_THICKNESS_SQUARED     = 1 << 16,
		CAVERN_EDGINESS              = 1 << 17,
		CAVERN_EDGINESS_SQUARED      = 1 << 18,

		SKYLAND_CELL                 = 1 << 19,
		SKYLAND_CENTER               = 1 << 20,
		SKYLAND_THICKNESS            = 1 << 21,
		SKYLAND_AUXILIARY_NOISE      = 1 << 22,
		SKYLAND_EDGINESS_SQUARED     = 1 << 23,
		SKYLAND_EDGINESS             = 1 << 24,
		SKYLAND_MIN_Y                = 1 << 25,
		SKYLAND_MAX_Y                = 1 << 26;

	public final OverworldSettings settings;
	public double
		hilliness,
		cliffiness,
		temperature,
		foliage,
		postCliffHeight,
		finalHeight,
		snowHeight,
		snowChance;
	public final double[] rawErosionAndSnow = new double[2];
	public CaveCell caveCell;
	public double[] caveNoise;
	public double
		caveSurfaceDepth,
		caveSystemEdginess,
		caveSystemEdginessSquared;
	public @Nullable IntArrayList caveFloors, caveCeilings;
	public CavernCell cavernCell;
	public double
		cavernCenter,
		cavernThicknessSquared,
		cavernEdginess,
		cavernEdginessSquared;
	public RegistryEntry<Biome> surfaceBiome;
	public double inLake = Double.NaN;
	public @Nullable SkylandCell skylandCell;
	public double
		skylandCenter,
		skylandThickness,
		skylandAuxiliaryNoise,
		skylandEdginessSquared,
		skylandEdginess,
		skylandMinY,
		skylandMaxY;

	public OverworldColumn(OverworldSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	//////////////////////////////// height ////////////////////////////////

	public double getSeaLevel() {
		return this.settings.height.sea_level();
	}

	public double getHilliness() {
		return (
			this.setFlag(HILLINESS)
			? this.hilliness = this.settings.height.hilliness().getValue(this.seed, this.x, this.z)
			: this.hilliness
		);
	}

	public double getCliffiness() {
		return (
			this.setFlag(CLIFFINESS)
			? this.cliffiness = this.computeCliffiness()
			: this.cliffiness
		);
	}

	public double computeCliffiness() {
		OverworldCliffSettings cliffs = this.settings.height.cliffs();
		return cliffs == null ? Double.NaN : cliffs.cliffiness().getValue(this.seed, this.x, this.z);
	}

	public double[] getRawErosionAndSnow() {
		if (this.setFlag(RAW_EROSION_AND_SNOW)) {
			ScriptedGrid.SECRET_COLUMN.accept(this, (OverworldColumn self) -> {
				self.settings.height.getErosionAndSnow(
					self.seed,
					self.x,
					self.z,
					self.getHilliness(),
					self.rawErosionAndSnow
				);
			});
		}
		return this.rawErosionAndSnow;
	}

	public double getRawErosion() {
		return this.getRawErosionAndSnow()[0];
	}

	public double getRawSnow() {
		return this.getRawErosionAndSnow()[1];
	}

	public double getSnowHeight() {
		return (
			this.setFlag(SNOW_HEIGHT)
			? this.snowHeight = this.computeSnowHeight()
			: this.snowHeight
		);
	}

	public double computeSnowHeight() {
		double snowHeight = this.applyCliffs(this.getRawSnow() * this.getHilliness());
		snowHeight += snowHeight * (1.0D / 64.0); //higher Y levels = more snow.
		snowHeight -= 256.0D / 64.0D; //less snow (manual bias).
		snowHeight -= this.getTemperature() * this.settings.miscellaneous.snow_temperature_multiplier(); //lower temperature = more snow.
		double finalHeight = this.getFinalTopHeightD();
		this.snowChance = snowHeight - finalHeight;
		if (finalHeight - this.getSeaLevel() < 32.0D) {
			return Interpolator.mixLinear(
				finalHeight,
				snowHeight,
				Interpolator.smoothClamp(
					(finalHeight - this.getSeaLevel()) * (1.0D / 32.0D)
				)
			);
		}
		return snowHeight;
	}

	public double getSnowChance() {
		this.getSnowHeight();
		return this.snowChance;
	}

	public double getPreCliffHeight() {
		return this.getRawErosion() * this.getHilliness();
	}

	public double getPostCliffHeight() {
		return (
			this.setFlag(POST_CLIFF_HEIGHT)
			? this.postCliffHeight = this.applyCliffs(this.getPreCliffHeight())
			: this.postCliffHeight
		);
	}

	public static double halfCliffCurve(double height, double coefficient) {
		double product = coefficient * height;
		return (product + height) / (product + 1.0D);
	}

	public static double fullCliffCurve(double height, double coefficient) {
		return (
			height <= 0.5D
			?        halfCliffCurve(       2.0D * height, coefficient) * 0.5D
			: 1.0D - halfCliffCurve(2.0D - 2.0D * height, coefficient) * 0.5D
		);
	}

	public double interpolateCliffs(OverworldCliffSettings cliffs, int floor, int ceil, double frac) {
		return Interpolator.mixLinear(
			cliffs.shelf_height().getValue(Permuter.permute(this.seed, floor), this.x, this.z) + floor,
			cliffs.shelf_height().getValue(Permuter.permute(this.seed,  ceil), this.x, this.z) + ceil,
			frac
		);
	}

	public double applyCliffs(double height) {
		OverworldCliffSettings cliffs = this.settings.height.cliffs();
		if (cliffs == null) return height;

		double scale = cliffs.scale() * this.getHilliness();
		double cliffiness = this.getCliffiness();
		height /= scale;

		int    floor = BigGlobeMath.floorI(height);
		int    ceil  = floor + 1;
		double mod   = height - floor;
		double curve = fullCliffCurve(mod, -cliffiness);
		double newHeight = Interpolator.mixLinear(height, this.interpolateCliffs(cliffs, floor, ceil, curve), cliffiness);
		return newHeight * scale;
	}

	@Override
	public double getFinalTopHeightD() {
		return (
			this.setFlag(FINAL_HEIGHT)
			? this.finalHeight = this.getPostCliffHeight()
			: this.finalHeight
		);
	}

	@Override
	public double getFinalBottomHeightD() {
		return this.settings.height.min_y();
	}

	@Override
	public int getFinalBottomHeightI() {
		return this.settings.height.min_y();
	}

	//////////////////////////////// other noise ////////////////////////////////

	public double getTemperature() {
		return (
			this.setFlag(TEMPERATURE)
			? this.temperature = ScriptedGrid.SECRET_COLUMN.apply(this, (OverworldColumn self) -> this.settings.temperature.noise().getValue(Permuter.stafford(self.seed), self.x, self.z))
			: this.temperature
		);
	}

	public double getHeightAdjustedTemperature(double y) {
		return this.settings.temperature.height_adjustment().evaluate(this.getTemperature(), this.getSeaLevel(), y);
	}

	public double getSurfaceTemperature() {
		return this.getHeightAdjustedTemperature(this.getFinalTopHeightD());
	}

	public double getFoliage() {
		return (
			this.setFlag(FOLIAGE)
			? this.foliage = ScriptedGrid.SECRET_COLUMN.apply(this, (OverworldColumn self) -> self.settings.foliage.noise().getValue(Permuter.stafford(self.seed), self.x, self.z))
			: this.foliage
		);
	}

	public double getHeightAdjustedFoliage(double y) {
		return this.settings.foliage.height_adjustment().evaluate(this.getFoliage(), this.getSeaLevel(), y);
	}

	public double getSurfaceFoliage() {
		return this.getHeightAdjustedFoliage(this.getFinalTopHeightD());
	}

	//////////////////////////////// caves ////////////////////////////////

	public void populateCaveFloorsAndCeilings() {
		CaveCell cell = this.getCaveCell();
		if (cell != null) {
			this.caveFloors = new IntArrayList(8);
			this.caveCeilings = new IntArrayList(8);
			double[] noise = this.caveNoise;
			assert noise != null;
			int depth = cell.settings.depth();
			int minY = this.getFinalTopHeightI() - depth;
			boolean previousCave = false;
			for (int index = 0; index < depth; index++) {
				int y = index + minY;
				boolean currentCave = noise[index] < cell.settings.getNoiseThreshold(this, y);
				if (currentCave && !previousCave) {
					this.caveFloors.add(y);
				}
				else if (previousCave && !currentCave) {
					this.caveCeilings.add(y - 1);
				}
				previousCave = currentCave;
			}
		}
	}

	public long getCaveSeed() {
		CaveCell cell = this.getCaveCell();
		return cell == null ? this.seed : cell.voronoiCell.center.getSeed(0xFCB66693C89B11E1L);
	}

	public double @Nullable [] getCaveNoise() {
		if (this.setFlag(CAVE_NOISE)) {
			CaveCell cell = this.getCaveCell();
			if (cell != null) ScriptedGrid.SECRET_COLUMN.accept(this, self -> cell.settings.getBulkY(self));
		}
		return this.caveNoise;
	}

	public double getCaveNoise(int y, boolean cache) {
		if (cache || this.hasFlag(CAVE_NOISE)) {
			double[] noise = this.getCaveNoise();
			if (noise == null) return Double.NaN;
			int index = y - (this.getFinalTopHeightI() - noise.length);
			if (index < 0 || index >= noise.length) return Double.NaN;
			return noise[index];
		}
		else {
			CaveCell cell = this.getCaveCell();
			if (cell == null) return Double.NaN;
			return ScriptedGrid.SECRET_COLUMN.get(this, () -> cell.settings.getValue(this, y));
		}
	}

	public double getCaveNoise(double y) {
		return this.getCaveNoise(BigGlobeMath.floorI(y), false);
	}

	public double getCachedCaveNoise(double y) {
		return this.getCaveNoise(BigGlobeMath.floorI(y), true);
	}

	public double getCaveDepth() {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.settings.depth();
	}

	public double getCaveNoiseThreshold(double y) {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.settings.getNoiseThreshold(this, y);
	}

	public double getCaveEffectiveWidth(double y) {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.settings.getEffectiveWidth(this, y);
	}

	public boolean isCaveAt(int y, boolean cache) {
		double noise = this.getCaveNoise(y, cache);
		if (Double.isNaN(noise)) return false;
		return noise < this.getCaveNoiseThreshold(y);
	}

	public double getCaveSurfaceDepth() {
		return (
			this.setFlag(CAVE_SURFACE_DEPTH)
			? this.caveSurfaceDepth = this.computeCaveSurfaceDepth()
			: this.caveSurfaceDepth
		);
	}

	public double computeCaveSurfaceDepth() {
		CaveCell cell = this.getCaveCell();
		if (cell == null) return Double.NaN;
		Grid2D noise = cell.settings.surface_depth_noise();
		if (noise == null) return Double.NaN;
		return noise.getValue(this.seed, this.x, this.z);
	}

	public double getNormalizedCaveSurfaceDepth() {
		double depth = this.getCaveSurfaceDepth();
		if (Double.isNaN(depth)) return Double.NaN;
		return depth / this.getCaveCell().settings.surface_depth_noise().maxValue();
	}

	public @Nullable CaveCell getCaveCell() {
		return this.setFlag(CAVE_CELL) ? this.computeCaveCell() : this.caveCell;
	}

	public @Nullable CaveCell computeCaveCell() {
		OverworldCaveSettings globalCaves = this.settings.underground.caves();
		if (globalCaves == null) return null;
		CaveCell caveCell = this.caveCell;
		VoronoiDiagram2D.Cell voronoiCell = globalCaves.placement.getNearestCell(this.x, this.z, caveCell != null ? caveCell.voronoiCell : null);
		if (caveCell == null) {
			caveCell = this.caveCell = new CaveCell();
		}
		else if (caveCell.voronoiCell.center.cellEquals(voronoiCell.center)) {
			return caveCell;
		}
		caveCell.voronoiCell = voronoiCell;
		caveCell.settings = globalCaves.templates.getRandomElement(voronoiCell.center.getSeed(0x1E5D30AEB917D0BFL));
		return caveCell;
	}

	public double getCaveSystemCenterX() {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerX;
	}

	public double getCaveSystemCenterZ() {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerZ;
	}

	public static String debugCaveSystemCenterX(CustomDisplayContext context) {
		CaveCell cell = context.<OverworldColumn>column().getCaveCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerX) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public static String debugCaveSystemCenterZ(CustomDisplayContext context) {
		CaveCell cell = context.<OverworldColumn>column().getCaveCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerZ) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public double getCaveSystemEdginessSquared() {
		return (
			this.setFlag(CAVE_SYSTEM_EDGINESS_SQUARED)
			? this.caveSystemEdginessSquared = this.computeCaveSystemEdginessSquared()
			: this.caveSystemEdginessSquared
		);
	}

	public double computeCaveSystemEdginessSquared() {
		CaveCell cell = this.getCaveCell();
		return cell == null ? Double.NaN : cell.voronoiCell.progressToEdgeSquaredD(this.x, this.z);
	}

	public double getCaveSystemEdginess() {
		return (
			this.setFlag(CAVE_SYSTEM_EDGINESS)
			? this.caveSystemEdginess = Math.sqrt(this.getCaveSystemEdginessSquared())
			: this.caveSystemEdginess
		);
	}

	//////////////////////////////// caverns ////////////////////////////////

	public @Nullable CavernCell getCavernCell() {
		return this.setFlag(CAVERN_CELL) ? this.computeCavernCell() : this.cavernCell;
	}

	public @Nullable CavernCell computeCavernCell() {
		OverworldCavernSettings globalCaverns = this.settings.underground.deep_caverns();
		if (globalCaverns == null) return null;
		CavernCell cavernCell = this.cavernCell;
		VoronoiDiagram2D.Cell voronoiCell = globalCaverns.placement.getNearestCell(this.x, this.z, cavernCell != null ? cavernCell.voronoiCell : null);
		if (cavernCell == null) {
			cavernCell = this.cavernCell = new CavernCell();
		}
		else if (cavernCell.voronoiCell.center.cellEquals(voronoiCell.center)) {
			return cavernCell;
		}
		cavernCell.voronoiCell = voronoiCell;
		LocalCavernSettings local = globalCaverns.templates.getRandomElement(voronoiCell.center.getSeed(0x4E68064756FB1FB7L));
		cavernCell.settings = local;
		cavernCell.averageCenter = local.average_center().get(voronoiCell.center.getSeed(0x649B8B0255A6DB63L));
		return cavernCell;
	}

	public double getCavernCenterX() {
		CavernCell cell = this.getCavernCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerX;
	}

	public double getCavernCenterZ() {
		CavernCell cell = this.getCavernCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerZ;
	}

	public static String debugCavernCenterX(CustomDisplayContext context) {
		CavernCell cell = context.<OverworldColumn>column().getCavernCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerX) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public static String debugCavernCenterZ(CustomDisplayContext context) {
		CavernCell cell = context.<OverworldColumn>column().getCavernCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerZ) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public double getCavernEdginessSquared() {
		return (
			this.setFlag(CAVERN_EDGINESS_SQUARED)
			? this.cavernEdginessSquared = this.computeCavernEdginessSquared()
			: this.cavernEdginessSquared
		);
	}

	public double computeCavernEdginessSquared() {
		CavernCell cell = this.getCavernCell();
		return cell == null ? Double.NaN : cell.voronoiCell.progressToEdgeSquaredD(this.x, this.z);
	}

	public double getCavernEdginess() {
		return (
			this.setFlag(CAVERN_EDGINESS)
			? this.cavernEdginess = Math.sqrt(this.getCavernEdginessSquared())
			: this.cavernEdginess
		);
	}

	public double getCavernAverageCenter() {
		CavernCell cell = this.getCavernCell();
		return cell != null ? cell.averageCenter : Double.NaN;
	}

	public double getCavernCenter() {
		return (
			this.setFlag(CAVERN_CENTER)
			? this.cavernCenter = this.computeCavernCenter()
			: this.cavernCenter
		);
	}

	public double computeCavernCenter() {
		CavernCell cell = this.getCavernCell();
		if (cell == null) return Double.NaN;
		return cell.averageCenter + cell.settings.center().getValue(this.seed, this.x, this.z);
	}

	public double getCavernThicknessSquared() {
		return (
			this.setFlag(CAVERN_THICKNESS_SQUARED)
			? this.cavernThicknessSquared = this.computeCavernThicknessSquared()
			: this.cavernThicknessSquared
		);
	}

	public double computeCavernThicknessSquared() {
		CavernCell cell = this.getCavernCell();
		if (cell == null) return Double.NaN;
		double thickness = cell.settings.thickness().getValue(this.seed, this.x, this.z);
		OverworldCavernSettings settings = this.settings.underground.deep_caverns();
		assert settings != null : "Have cell, but no settings?";

		double progress = cell.voronoiCell.progressToEdgeD(this.x, this.z);
		double threshold = 1.0D - cell.settings.padding() / (settings.placement.distance * 0.5D);
		double fraction = Interpolator.unmixLinear(threshold, 1.0D, progress);
		if (fraction > 0.0D) {
			thickness -= BigGlobeMath.squareD(fraction) * cell.settings.thickness().maxValue();
		}

		OverworldCaveSettings caves = this.settings.underground.caves();
		if (caves != null) {
			double maxY = this.getCavernCenter() + cell.settings.sqrtMaxThickness();
			double space = this.getFinalTopHeightD() - caves.maxDepth;
			double verticalPenalty = BigGlobeMath.squareD(Math.max(Interpolator.unmixLinear(maxY + cell.settings.padding(), maxY, space), 0.0D));
			thickness -= verticalPenalty * cell.settings.thickness().maxValue();
		}

		return thickness;
	}

	public double getCavernThickness() {
		return Math.sqrt(this.getCavernThicknessSquared());
	}

	public boolean isCavernAt(int y) {
		return BigGlobeMath.squareD(y - this.getCavernCenter()) < this.getCavernThicknessSquared();
	}

	//////////////////////////////// skylands ////////////////////////////////

	public @Nullable SkylandCell getSkylandCell() {
		return this.setFlag(SKYLAND_CELL) ? this.computeSkylandCell() : this.skylandCell;
	}

	public @Nullable SkylandCell computeSkylandCell() {
		OverworldSkylandSettings globalSkylands = this.settings.skylands;
		if (globalSkylands == null) return null;
		SkylandCell skylandCell = this.skylandCell;
		VoronoiDiagram2D.Cell voronoiCell = globalSkylands.placement.getNearestCell(this.x, this.z, skylandCell != null ? skylandCell.voronoiCell : null);
		if (skylandCell == null) {
			skylandCell = this.skylandCell = new SkylandCell();
		}
		else if (skylandCell.voronoiCell.center.cellEquals(voronoiCell.center)) {
			return skylandCell;
		}
		skylandCell.voronoiCell = voronoiCell;
		LocalSkylandSettings local = globalSkylands.templates.getRandomElement(voronoiCell.center.getSeed(0x306A01988A92962CL));
		skylandCell.settings = local;
		skylandCell.averageCenter = local.average_center().get(voronoiCell.center.getSeed(0x7DE493A0E9989DA6L));
		return skylandCell;
	}

	public double getSkylandCenterX() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerX;
	}

	public double getSkylandCenterZ() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.voronoiCell.center.centerZ;
	}

	public static String debugSkylandCenterX(CustomDisplayContext context) {
		SkylandCell cell = context.<OverworldColumn>column().getSkylandCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerX) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public static String debugSkylandCenterZ(CustomDisplayContext context) {
		SkylandCell cell = context.<OverworldColumn>column().getSkylandCell();
		if (cell == null) return CustomDisplayContext.format(Double.NaN);
		SeedPoint seedPoint = cell.voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerZ) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public double getSkylandAverageCenter() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.averageCenter;
	}

	public double getSkylandCenter() {
		return (
			this.setFlag(SKYLAND_CENTER)
			? this.skylandCenter = this.computeSkylandCenter()
			: this.skylandCenter
		);
	}

	public double computeSkylandCenter() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.averageCenter + cell.settings.center().getValue(cell.voronoiCell.center.getSeed(this.seed), this.x, this.z);
	}

	public double getSkylandThickness() {
		return (
			this.setFlag(SKYLAND_THICKNESS)
			? this.skylandThickness = this.computeSkylandThickness()
			: this.skylandThickness
		);
	}

	public double computeSkylandThickness() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.settings.thickness().getValue(cell.voronoiCell.center.getSeed(this.seed), this.x, this.z);
	}

	public double getSkylandAuxiliaryNoise() {
		return (
			this.setFlag(SKYLAND_AUXILIARY_NOISE)
			? this.skylandAuxiliaryNoise = this.computeSkylandAuxiliaryNoise()
			: this.skylandAuxiliaryNoise
		);
	}

	public double computeSkylandAuxiliaryNoise() {
		SkylandCell cell = this.getSkylandCell();
		if (cell == null) return Double.NaN;
		Grid2D grid = cell.settings.auxiliary_noise();
		if (grid == null) return Double.NaN;
		return grid.getValue(cell.voronoiCell.center.getSeed(this.seed), this.x, this.z);
	}

	public double getSkylandEdginessSquared() {
		return (
			this.setFlag(SKYLAND_EDGINESS_SQUARED)
			? this.skylandEdginessSquared = this.computeSkylandBiasSquared()
			: this.skylandEdginessSquared
		);
	}

	public double computeSkylandBiasSquared() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.voronoiCell.progressToEdgeSquaredD(this.x, this.z);
	}

	public double getSkylandEdginess() {
		return (
			this.setFlag(SKYLAND_EDGINESS)
			? this.skylandEdginess = Math.sqrt(this.getSkylandEdginessSquared())
			: this.skylandEdginess
		);
	}

	public double getSkylandMinY() {
		return (
			this.setFlag(SKYLAND_MIN_Y)
			? this.skylandMinY = this.computeSkylandMinY()
			: this.skylandMinY
		);
	}

	public double computeSkylandMinY() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.settings.min_y().evaluate(this, cell.averageCenter);
	}

	public double getSkylandMaxY() {
		return (
			this.setFlag(SKYLAND_MAX_Y)
			? this.skylandMaxY = this.computeSkylandMaxY()
			: this.skylandMaxY
		);
	}

	public double computeSkylandMaxY() {
		SkylandCell cell = this.getSkylandCell();
		return cell == null ? Double.NaN : cell.settings.max_y().evaluate(this, cell.averageCenter);
	}

	public boolean hasSkyland() {
		return this.getSkylandMaxY() > this.getSkylandMinY();
	}

	//////////////////////////////// misc ////////////////////////////////

	public void populateInLake(LakeStructure.@Nullable Piece piece) {
		if (
			piece != null &&
			BigGlobeMath.squareD(
				this.x - piece.data.x(),
				this.z - piece.data.z()
			)
			< BigGlobeMath.squareD(piece.data.horizontal_radius())
		) {
			this.inLake = Interpolator.unmixSmooth(piece.data.y(), piece.data.y() - 4.0D, this.finalHeight);
			this.snowHeight = Interpolator.mixLinear(this.snowHeight, this.finalHeight, this.inLake);
		}
		else {
			this.inLake = 0.0D;
		}
	}

	public double getInLake() {
		double inLake = this.inLake;
		if (!Double.isNaN(inLake)) return inLake;
		else throw new IllegalStateException("inLake not yet populated!");
	}

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return this.settings.biomes.getBiome(this, y, this.seed);
	}

	@Override
	public RegistryEntry<Biome> getSurfaceBiome() {
		return (
			this.setFlag(SURFACE_BIOME)
			? this.surfaceBiome = super.getSurfaceBiome()
			: this.surfaceBiome
		);
	}

	@Override
	public boolean isTerrainAt(int y, boolean cache) {
		return y >= this.getFinalBottomHeightI() && y < this.getFinalTopHeightI() && !this.isCaveAt(y, cache) && !this.isCavernAt(y);
	}

	@Override
	public OverworldColumn blankCopy() {
		return new OverworldColumn(this.settings, this.seed, this.x, this.z);
	}

	public static class CaveCell {

		public VoronoiDiagram2D.Cell voronoiCell;
		public LocalOverworldCaveSettings settings;
	}

	public static class CavernCell {

		public VoronoiDiagram2D.Cell voronoiCell;
		public double averageCenter;
		public LocalCavernSettings settings;
	}

	public static class SkylandCell {

		public VoronoiDiagram2D.Cell voronoiCell;
		public double averageCenter;
		public LocalSkylandSettings settings;
	}
}