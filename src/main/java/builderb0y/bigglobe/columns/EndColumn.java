package builderb0y.bigglobe.columns;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.noise.ScriptedGrid;
import builderb0y.bigglobe.settings.EndSettings;
import builderb0y.bigglobe.settings.EndSettings.BridgeCloudSettings;
import builderb0y.bigglobe.settings.EndSettings.RingCloudSettings;

public class EndColumn extends WorldColumn {

	public static final int
		WARP_X                   = 1 << 0,
		WARP_Z                   = 1 << 1,
		WARP_RADIUS              = 1 << 2,
		WARP_ANGLE               = 1 << 3,
		DISTANCE_TO_ORIGIN       = 1 << 4,
		MOUNTAIN_CENTER_Y        = 1 << 5,
		MOUNTAIN_THICKNESS       = 1 << 6,
		LOWER_RING_CLOUD_NOISE   = 1 << 7,
		UPPER_RING_CLOUD_NOISE   = 1 << 8,
		LOWER_BRIDGE_CLOUD_NOISE = 1 << 9,
		UPPER_BRIDGE_CLOUD_NOISE = 1 << 10;

	public final EndSettings settings;

	public double
		warpX,
		warpZ,
		warpRadius,
		warpAngle,
		distanceToOrigin,
		mountainCenterY,
		mountainThickness;
	public double[]
		lowerRingCloudNoise,
		upperRingCloudNoise,
		lowerBridgeCloudNoise,
		upperBridgeCloudNoise;

	public EndColumn(EndSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	//////////////////////////////// warp ////////////////////////////////

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

	public static String debug_distanceToOrigin(CustomDisplayContext context) {
		return CustomDisplayContext.format(context.<EndColumn>column().getDistanceToOrigin()) + " block(s) " + context.arrow(0, 0);
	}

	//////////////////////////////// mountains ////////////////////////////////

	public double getMountainCenterY() {
		return (
			this.setFlag(MOUNTAIN_CENTER_Y)
			? this.mountainCenterY = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				self -> self.settings.mountains().center_y().getValue(self.seed, self.x, self.z)
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

	//////////////////////////////// ring clouds ////////////////////////////////

	public double getRingCloudHorizontalBias() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Double.NaN;
		double mid       = (ringCloudSettings.max_radius() + ringCloudSettings.min_radius()) * 0.5D;
		double halfRange = (ringCloudSettings.max_radius() - ringCloudSettings.min_radius()) * 0.5D;
		return BigGlobeMath.squareD((this.getWarpRadius() - mid) / halfRange) * -ringCloudSettings.noise().maxValue();
	}

	//////////////// lower ////////////////

	public double getLowerRingCloudVerticalBias(double y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Double.NaN;
		double mountainCenterY = this.getMountainCenterY();
		double centerY = mountainCenterY - ringCloudSettings.center_y();
		return BigGlobeMath.squareD((y - centerY) / ringCloudSettings.vertical_thickness()) * -ringCloudSettings.noise().maxValue();
	}

	public int getLowerRingCloudSampleStartY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		return BigGlobeMath.ceilI(this.getMountainCenterY() - ringCloudSettings.center_y() - ringCloudSettings.vertical_thickness());
	}

	public int getLowerRingCloudSampleEndY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		return BigGlobeMath.ceilI(this.getMountainCenterY() - ringCloudSettings.center_y() + ringCloudSettings.vertical_thickness());
	}

	public double @Nullable [] getLowerRingCloudNoise() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return null;
		double horizontalBias = this.getRingCloudHorizontalBias();
		if (horizontalBias <= -ringCloudSettings.noise().maxValue()) return null;
		double[] lowerRingCloudNoise = this.lowerRingCloudNoise;
		if (lowerRingCloudNoise == null) {
			lowerRingCloudNoise = this.lowerRingCloudNoise = new double[ringCloudSettings.verticalSamples()];
		}
		if (this.setFlag(LOWER_RING_CLOUD_NOISE)) {
			int startY = this.getLowerRingCloudSampleStartY();
			ringCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, lowerRingCloudNoise, lowerRingCloudNoise.length);
			for (int index = 0, length = lowerRingCloudNoise.length; index < length; index++) {
				lowerRingCloudNoise[index] += horizontalBias + this.getLowerRingCloudVerticalBias(index + startY);
			}
		}
		return lowerRingCloudNoise;
	}

	//////////////// upper ////////////////

	public double getUpperRingCloudVerticalBias(double y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Double.NaN;
		double mountainCenterY = this.getMountainCenterY();
		double centerY = mountainCenterY + ringCloudSettings.center_y();
		return BigGlobeMath.squareD((y - centerY) / ringCloudSettings.vertical_thickness()) * -ringCloudSettings.noise().maxValue();
	}

	public int getUpperRingCloudSampleStartY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		return BigGlobeMath.ceilI(this.getMountainCenterY() + ringCloudSettings.center_y() - ringCloudSettings.vertical_thickness());
	}

	public int getUpperRingCloudSampleEndY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		return BigGlobeMath.ceilI(this.getMountainCenterY() + ringCloudSettings.center_y() + ringCloudSettings.vertical_thickness());
	}

	public double @Nullable [] getUpperRingCloudNoise() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds();
		if (ringCloudSettings == null) return null;
		double horizontalBias = this.getRingCloudHorizontalBias();
		if (horizontalBias <= -ringCloudSettings.noise().maxValue()) return null;
		double[] upperRingCloudNoise = this.upperRingCloudNoise;
		if (upperRingCloudNoise == null) {
			upperRingCloudNoise = this.upperRingCloudNoise = new double[ringCloudSettings.verticalSamples()];
		}
		if (this.setFlag(UPPER_RING_CLOUD_NOISE)) {
			int startY = this.getUpperRingCloudSampleStartY();
			ringCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, upperRingCloudNoise, upperRingCloudNoise.length);
			for (int index = 0, length = upperRingCloudNoise.length; index < length; index++) {
				upperRingCloudNoise[index] += horizontalBias + this.getUpperRingCloudVerticalBias(index + startY);
			}
		}
		return upperRingCloudNoise;
	}

	//////////////////////////////// bridge clouds ////////////////////////////////

	public double getBridgeCloudHorizontalRadialBias() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		double radius = this.getWarpRadius();
		if (radius >= bridgeCloudSettings.mid_radius()) return 0.0D;
		double range = bridgeCloudSettings.mid_radius() - bridgeCloudSettings.min_radius();
		return BigGlobeMath.squareD((bridgeCloudSettings.mid_radius() - radius) / range) * -bridgeCloudSettings.noise().maxValue();
	}

	public double getBridgeCloudHorizontalAngularBias() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		double angle = this.getWarpAngle() * bridgeCloudSettings.count();
		angle += Permuter.nextPositiveDouble(this.seed ^ 0x39BF90041EA9E0D0L) * BigGlobeMath.TAU;
		return (Math.sin(angle) * 0.5D + 0.5D) * -bridgeCloudSettings.noise().maxValue();
	}

	public double getBridgeCloudHorizontalCombinedRadius() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		double angle = this.getWarpAngle() * bridgeCloudSettings.count();
		angle += Permuter.nextPositiveDouble(this.seed ^ 0x39BF90041EA9E0D0L) * BigGlobeMath.TAU;
		double bias = Math.sin(angle) * 0.5D + 0.5D;
		double radius = this.getWarpRadius();
		if (radius < bridgeCloudSettings.mid_radius()) {
			double range = bridgeCloudSettings.mid_radius() - bridgeCloudSettings.min_radius();
			bias += BigGlobeMath.squareD((bridgeCloudSettings.mid_radius() - radius) / range);
		}
		return -bias * bridgeCloudSettings.noise().maxValue();
	}

	//////////////// lower ////////////////

	public double getLowerBridgeCloudCenterY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		return this.getMountainCenterY() - bridgeCloudSettings.base_y() - bridgeCloudSettings.archness() * this.getWarpRadius();
	}

	public double getLowerBridgeCloudVerticalBias(double y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		double centerY = this.getMountainCenterY() - bridgeCloudSettings.base_y() - bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.squareD((y - centerY) / bridgeCloudSettings.vertical_thickness()) * -bridgeCloudSettings.noise().maxValue();
	}

	public int getLowerBridgeCloudSampleStartY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getMountainCenterY() - bridgeCloudSettings.base_y() - bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.ceilI(centerY - bridgeCloudSettings.vertical_thickness());
	}

	public int getLowerBridgeCloudSampleEndY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getMountainCenterY() - bridgeCloudSettings.base_y() - bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.ceilI(centerY + bridgeCloudSettings.vertical_thickness());
	}

	public double @Nullable [] getLowerBridgeCloudNoise() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return null;
		double horizontalBias = this.getBridgeCloudHorizontalCombinedRadius();
		if (horizontalBias <= -bridgeCloudSettings.noise().maxValue()) return null;
		double[] lowerBridgeCloudNoise = this.lowerBridgeCloudNoise;
		if (lowerBridgeCloudNoise == null) {
			lowerBridgeCloudNoise = this.lowerBridgeCloudNoise = new double[bridgeCloudSettings.verticalSamples()];
		}
		if (this.setFlag(LOWER_BRIDGE_CLOUD_NOISE)) {
			int startY = this.getLowerBridgeCloudSampleStartY();
			bridgeCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, lowerBridgeCloudNoise, lowerBridgeCloudNoise.length);
			for (int index = 0, length = lowerBridgeCloudNoise.length; index < length; index++) {
				lowerBridgeCloudNoise[index] += horizontalBias + this.getLowerBridgeCloudVerticalBias(index + startY);
			}
		}
		return lowerBridgeCloudNoise;
	}

	//////////////// upper ////////////////

	public double getUpperBridgeCloudCenterY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		return this.getMountainCenterY() + bridgeCloudSettings.base_y() + bridgeCloudSettings.archness() * this.getWarpRadius();
	}

	public double getUpperBridgeCloudVerticalBias(double y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Double.NaN;
		double centerY = this.getMountainCenterY() + bridgeCloudSettings.base_y() + bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.squareD((y - centerY) / bridgeCloudSettings.vertical_thickness()) * -bridgeCloudSettings.noise().maxValue();
	}

	public int getUpperBridgeCloudSampleStartY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getMountainCenterY() + bridgeCloudSettings.base_y() + bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.ceilI(centerY - bridgeCloudSettings.vertical_thickness());
	}

	public int getUpperBridgeCloudSampleEndY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getMountainCenterY() + bridgeCloudSettings.base_y() + bridgeCloudSettings.archness() * this.getWarpRadius();
		return BigGlobeMath.ceilI(centerY + bridgeCloudSettings.vertical_thickness());
	}

	public double @Nullable [] getUpperBridgeCloudNoise() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds();
		if (bridgeCloudSettings == null) return null;
		double horizontalBias = this.getBridgeCloudHorizontalCombinedRadius();
		if (horizontalBias <= -bridgeCloudSettings.noise().maxValue()) return null;
		double[] upperBridgeCloudNoise = this.upperBridgeCloudNoise;
		if (upperBridgeCloudNoise == null) {
			upperBridgeCloudNoise = this.upperBridgeCloudNoise = new double[bridgeCloudSettings.verticalSamples()];
		}
		if (this.setFlag(UPPER_BRIDGE_CLOUD_NOISE)) {
			int startY = this.getUpperBridgeCloudSampleStartY();
			bridgeCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, upperBridgeCloudNoise, upperBridgeCloudNoise.length);
			for (int index = 0, length = upperBridgeCloudNoise.length; index < length; index++) {
				upperBridgeCloudNoise[index] += horizontalBias + this.getUpperBridgeCloudVerticalBias(index + startY);
			}
		}
		return upperBridgeCloudNoise;
	}

	//////////////////////////////// misc ////////////////////////////////

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