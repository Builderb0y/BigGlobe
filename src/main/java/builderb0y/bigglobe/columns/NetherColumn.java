package builderb0y.bigglobe.columns;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.settings.NetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.NetherSettings.NetherCavernSettings;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.SeedPoint;

public class NetherColumn extends WorldColumn {

	public static final int
		LOCAL_CELL             = 1 << 0,
		CAVE_NOISE             = 1 << 1,
		CAVERN_NOISE           = 1 << 2,
		BIOME_EDGINESS         = 1 << 3,
		BIOME_EDGINESS_SQUARED = 1 << 4;

	public final NetherSettings settings;
	public LocalCell localCell;
	public double[] caveNoise, cavernNoise;
	public double edginess, edginessSquared;

	public IntList caveFloors, caveCeilings, cavernFloors, cavernCeilings;

	public NetherColumn(NetherSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	public void populateCaveAndCavernFloors() {
		this.    caveFloors = new IntArrayList(16);
		this.  caveCeilings = new IntArrayList(16);
		this.  cavernFloors = new IntArrayList( 8);
		this.cavernCeilings = new IntArrayList( 8);
		{
			double[] caveNoise = this.caveNoise;
			int minY = this.settings.min_y();
			int maxY = this.settings.max_y();
			ColumnYToDoubleScript.Holder widthScript = this.getLocalCell().settings.caves().width();
			boolean previousCave = false;
			for (int y = minY; y < maxY; y++) {
				int index = y - minY;
				boolean currentCave = caveNoise[index] < BigGlobeMath.squareD(widthScript.evaluate(this, y));
				if (currentCave && !previousCave) {
					this.caveFloors.add(y);
				}
				else if (!currentCave && previousCave) {
					this.caveCeilings.add(y - 1);
				}
				previousCave = currentCave;
			}
		}
		{
			double[] cavernNoise = this.cavernNoise;
			NetherCavernSettings caverns = this.getLocalCell().settings.caverns();
			int minY = caverns.min_y();
			int maxY = caverns.max_y();
			boolean previousCavern = false;
			for (int y = minY; y < maxY; y++) {
				int index = y - minY;
				boolean currentCavern = cavernNoise[index] < 0.0D;
				if (currentCavern && !previousCavern) {
					this.cavernFloors.add(y);
				}
				else if (!currentCavern && previousCavern) {
					this.cavernCeilings.add(y - 1);
				}
				previousCavern = currentCavern;
			}
		}
	}

	public double getBiomeCenterX() {
		return this.getLocalCell().voronoiCell.center.centerX;
	}

	public double getBiomeCenterZ() {
		return this.getLocalCell().voronoiCell.center.centerZ;
	}

	public static String debugBiomeCenterX(CustomDisplayContext context) {
		SeedPoint seedPoint = context.<NetherColumn>column().getLocalCell().voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerX) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public static String debugBiomeCenterZ(CustomDisplayContext context) {
		SeedPoint seedPoint = context.<NetherColumn>column().getLocalCell().voronoiCell.center;
		return CustomDisplayContext.format(seedPoint.centerZ) + " (" + context.distance(seedPoint.centerX, seedPoint.centerZ) + " blocks " + context.arrow(seedPoint.centerX, seedPoint.centerZ) + ')';
	}

	public double getEdginessSquared() {
		return (
			this.setFlag(BIOME_EDGINESS_SQUARED)
			? this.edginessSquared = this.getLocalCell().voronoiCell.progressToEdgeSquaredD(this.x, this.z)
			: this.edginessSquared
		);
	}

	public double getEdginess() {
		return (
			this.setFlag(BIOME_EDGINESS)
			? this.edginess = Math.sqrt(this.getEdginessSquared())
			: this.edginess
		);
	}

	public double getLavaLevel() {
		return this.getLocalCell().lavaLevel;
	}

	public double[] getCaveNoise() {
		return (
			this.setFlag(CAVE_NOISE)
			? this.computeCaveNoise()
			: this.caveNoise
		);
	}

	public double getCaveNoise(int y, boolean cache) {
		double[] noise = cache ? this.computeCaveNoise() : this.caveNoise;
		if (noise != null) {
			int index = y - this.settings.min_y();
			if (index >= 0 && index < noise.length) {
				return noise[index];
			}
		}
		LocalCell cell = this.getLocalCell();
		return cell.settings.caves().noise().getValue(cell.voronoiCell.center.getSeed(this.seed ^ 0xCACD037B0560050BL), this.x, y, this.z);
	}

	public double getCaveNoise(double y) {
		return this.getCaveNoise(BigGlobeMath.floorI(y), false);
	}

	public double getCaveWidth(double y) {
		return this.getLocalCell().settings.caves().width().evaluate(this, y);
	}

	public double getCaveWidthSquared(double y) {
		return BigGlobeMath.squareD(this.getCaveWidth(y));
	}

	public double[] computeCaveNoise() {
		int range = this.settings.max_y() - this.settings.min_y();
		double[] noise = this.caveNoise;
		if (noise == null || noise.length < range) {
			noise = this.caveNoise = new double[range];
		}
		LocalCell cell = this.getLocalCell();
		cell.settings.caves().noise().getBulkY(
			cell.voronoiCell.center.getSeed(this.seed ^ 0xCACD037B0560050BL),
			this.x,
			this.settings.min_y(),
			this.z,
			noise,
			range
		);
		return noise;
	}

	public double[] getCavernNoise() {
		return (
			this.setFlag(CAVERN_NOISE)
			? this.computeCavernNoise()
			: this.cavernNoise
		);
	}

	public double getCavernNoise(int y, boolean cache) {
		double[] noise = cache ? this.getCavernNoise() : this.cavernNoise;
		if (noise != null) {
			int index = y - this.getLocalCell().settings.caverns().min_y();
			if (index >= 0 && index < noise.length) {
				return noise[index];
			}
		}
		LocalCell cell = this.getLocalCell();
		return cell.settings.caverns().noise().getValue(cell.voronoiCell.center.getSeed(this.seed ^ 0x4E5DCB0DE78F7512L), this.x, y, this.z);
	}

	public double getCavernNoise(double y) {
		return this.getCavernNoise(BigGlobeMath.floorI(y), false);
	}

	public double[] computeCavernNoise() {
		LocalCell cell = this.getLocalCell();
		int range = cell.settings.caverns().max_y() - cell.settings.caverns().min_y();
		double[] noise = this.cavernNoise;
		if (noise == null) {
			noise = this.cavernNoise = new double[range];
		}
		cell.settings.caverns().noise().getBulkY(
			cell.voronoiCell.center.getSeed(this.seed ^ 0x4E5DCB0DE78F7512L),
			this.x,
			cell.settings.caverns().min_y(),
			this.z,
			noise,
			range
		);
		return noise;
	}

	public LocalCell getLocalCell() {
		return (
			this.setFlag(LOCAL_CELL)
			? this.computeLocalCell()
			: this.localCell
		);
	}

	public LocalCell computeLocalCell() {
		LocalCell localCell = this.localCell;
		VoronoiDiagram2D.Cell voronoiCell = this.settings.biome_placement().getNearestCell(this.x, this.z, localCell != null ? localCell.voronoiCell : null);
		if (localCell == null) {
			localCell = this.localCell = new LocalCell();
		}
		else if (localCell.voronoiCell.center.cellEquals(voronoiCell.center)) {
			return localCell;
		}
		localCell.voronoiCell = voronoiCell;
		localCell.settings = DelegatingContainedRandomList.from(this.settings.local_settings().elements).getRandomElement(voronoiCell.center.getSeed(0x3609EABAE4B4765CL));
		localCell.lavaLevel = BigGlobeMath.ceilI(localCell.settings.fluid_level().get(voronoiCell.center.getSeed(0xEAC12131FA3753DEL)));
		return localCell;
	}

	@Override
	public double getFinalTopHeightD() {
		return this.settings.max_y();
	}

	@Override
	public int getFinalTopHeightI() {
		return this.settings.max_y();
	}

	@Override
	public double getFinalBottomHeightD() {
		return this.settings.min_y();
	}

	@Override
	public int getFinalBottomHeightI() {
		return this.settings.min_y();
	}

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return this.getLocalCell().settings.biome();
	}

	@Override
	public WorldColumn blankCopy() {
		return new NetherColumn(this.settings, this.seed, this.x, this.z);
	}

	public static class LocalCell {

		public VoronoiDiagram2D.Cell voronoiCell;
		public LocalNetherSettings settings;
		public int lavaLevel;
	}
}