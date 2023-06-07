package builderb0y.bigglobe.columns;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.ScriptedGrid;
import builderb0y.bigglobe.settings.EndSettings;

public class EndColumn extends WorldColumn {

	public static final int
		WARP_X             = 1 << 0,
		WARP_Z             = 1 << 1,
		WARP_RADIUS        = 1 << 2,
		WARP_ANGLE         = 1 << 3,
		DISTANCE_TO_ORIGIN = 1 << 4,
		MOUNTAIN_CENTER_Y  = 1 << 5,
		MOUNTAIN_THICKNESS = 1 << 6;

	public final EndSettings settings;

	public double
		warpX,
		warpZ,
		warpRadius,
		warpAngle,
		distanceToOrigin,
		mountainCenterY,
		mountainThickness;

	public EndColumn(EndSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	public double getWarpX() {
		return (
			this.setFlag(WARP_X)
			? this.warpX = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				self -> self.settings.warp_x().getValue(self.seed, self.x, self.z)
			)
			: this.warpX
		);
	}

	public double getWarpZ() {
		return (
			this.setFlag(WARP_Z)
			? this.warpZ = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				self -> self.settings.warp_z().getValue(self.seed, self.x, self.z)
			)
			: this.warpZ
		);
	}

	public double getWarpRadius() {
		return this.setFlag(WARP_RADIUS) ? this.warpRadius = Math.sqrt(BigGlobeMath.squareD(this.getWarpX(), this.getWarpZ())) : this.warpRadius;
	}

	public double getWarpAngle() {
		return this.setFlag(WARP_ANGLE) ? this.warpAngle = Math.atan2(this.getWarpZ(), this.getWarpX()) : this.warpAngle;
	}

	public double getDistanceToOrigin() {
		return this.setFlag(DISTANCE_TO_ORIGIN) ? this.distanceToOrigin = Math.sqrt(BigGlobeMath.squareD(this.x, this.z)) : this.distanceToOrigin;
	}

	public double getMountainCenterY() {
		return (
			this.setFlag(MOUNTAIN_CENTER_Y)
			? this.mountainCenterY = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				self -> self.settings.mountains().noise().getValue(self.seed, self.x, self.z)
			)
			: this.mountainCenterY
		);
	}

	public double getMountainThickness() {
		return (
			this.setFlag(MOUNTAIN_THICKNESS)
			? this.mountainThickness = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				self -> self.settings.mountains().thickness().evaluate(self, self.getMountainCenterY())
			)
			: this.mountainThickness
		);
	}

	@Override
	public double getFinalTopHeightD() {
		return this.getMountainCenterY() + this.getMountainThickness();
	}

	@Override
	public double getFinalBottomHeightD() {
		return this.getMountainCenterY() - this.getMountainThickness();
	}

	@Override
	public boolean hasTerrain() {
		return this.getMountainThickness() > 0.0D;
	}

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return this.hasTerrain() ? this.settings.biomes().the_end : this.settings.biomes().the_void;
	}

	@Override
	public boolean isTerrainAt(int y, boolean cache) {
		return this.hasTerrain() && y >= this.getFinalBottomHeightI() && y < this.getFinalTopHeightI();
	}

	@Override
	public WorldColumn blankCopy() {
		return new EndColumn(this.settings, this.seed, this.x, this.z);
	}
}