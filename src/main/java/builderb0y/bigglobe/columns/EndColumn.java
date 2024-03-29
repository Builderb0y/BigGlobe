package builderb0y.bigglobe.columns;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.noise.ScriptedGrid;
import builderb0y.bigglobe.settings.EndSettings;
import builderb0y.bigglobe.settings.EndSettings.BridgeCloudSettings;
import builderb0y.bigglobe.settings.EndSettings.RingCloudSettings;

public class EndColumn extends WorldColumn {

	public static final int
		WARP_X                     = 1 << 0,
		WARP_Z                     = 1 << 1,
		WARP_RADIUS                = 1 << 2,
		WARP_ANGLE                 = 1 << 3,
		DISTANCE_TO_ORIGIN         = 1 << 4,
		ANGLE_TO_ORIGIN            = 1 << 5,
		NEST_NOISE                 = 1 << 6,
		MOUNTAIN_CENTER_Y          = 1 << 7,
		MOUNTAIN_THICKNESS         = 1 << 8,
		FOLIAGE                    = 1 << 9,
		LOWER_RING_CLOUD_NOISE     = 1 << 10,
		UPPER_RING_CLOUD_NOISE     = 1 << 11,
		LOWER_BRIDGE_CLOUD_NOISE   = 1 << 12,
		UPPER_BRIDGE_CLOUD_NOISE   = 1 << 13,
		RING_CLOUD_HORIZONTAL_BIAS = 1 << 14,
		BRIDGE_CLOUD_ANGULAR_BIAS  = 1 << 15,
		BRIDGE_CLOUD_RADIAL_BIAS   = 1 << 16,
		BRIDGE_CLOUD_ARCHNESS      = 1 << 17;

	public final EndSettings settings;

	public double
		warpX,
		warpZ,
		warpRadius,
		warpAngle,
		distanceToOrigin,
		angleToOrigin,
		mountainCenterY,
		mountainThickness,
		foliage,
		ringCloudHorizontalBias,
		bridgeCloudAngularBias,
		bridgeCloudRadialBias,
		bridgeCloudArchness;
	public @Nullable NumberArray
		nestNoise,
		lowerRingCloudNoise,
		upperRingCloudNoise,
		lowerBridgeCloudNoise,
		upperBridgeCloudNoise;
	public IntList
		nestFloorLevels,
		nestCeilingLevels,
		lowerRingCloudFloorLevels,
		lowerRingCloudCeilingLevels,
		upperRingCloudFloorLevels,
		upperRingCloudCeilingLevels,
		lowerBridgeCloudFloorLevels,
		lowerBridgeCloudCeilingLevels,
		upperBridgeCloudFloorLevels,
		upperBridgeCloudCeilingLevels;

	public EndColumn(EndSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	/**
	the logic for {@link #updateLevels()} needs to
	be ran 4 times for 4 different types of clouds,
	just assigning to different fields.
	so this class contains those fields and
	performs the relevant logic on them.
	I reckon this makes the code about twice as short as normal.
	it would be 4x, but there's some extra
	lines for storing the fields to operate on.
	*/
	public static class CloudLevelUpdater {

		public static final CloudLevelUpdater
			NEST         = new CloudLevelUpdater("getNestNoise",             "nestFloorLevels",             "nestCeilingLevels"            ),
			LOWER_RING   = new CloudLevelUpdater("getLowerRingCloudNoise",   "lowerRingCloudFloorLevels",   "lowerRingCloudCeilingLevels"  ),
			UPPER_RING   = new CloudLevelUpdater("getUpperRingCloudNoise",   "upperRingCloudFloorLevels",   "upperRingCloudCeilingLevels"  ),
			LOWER_BRIDGE = new CloudLevelUpdater("getLowerBridgeCloudNoise", "lowerBridgeCloudFloorLevels", "lowerBridgeCloudCeilingLevels"),
			UPPER_BRIDGE = new CloudLevelUpdater("getUpperBridgeCloudNoise", "upperBridgeCloudFloorLevels", "upperBridgeCloudCeilingLevels");

		public final MethodHandle noise;
		public final VarHandle floor, ceiling;

		public CloudLevelUpdater(String noise, String floor, String ceiling) {
			try {
				this.noise   = MethodHandles.lookup().findVirtual  (EndColumn.class, noise, MethodType.methodType(NumberArray.class));
				this.floor   = MethodHandles.lookup().findVarHandle(EndColumn.class, floor,   IntList.class);
				this.ceiling = MethodHandles.lookup().findVarHandle(EndColumn.class, ceiling, IntList.class);
			}
			catch (Exception exception) {
				throw AutoCodecUtil.rethrow(exception);
			}
		}

		public void update(EndColumn column, int minY) {
			NumberArray noise;
			try {
				noise = (NumberArray)(this.noise.invokeExact(column));
			}
			catch (Throwable throwable) {
				throw AutoCodecUtil.rethrow(throwable);
			}
			if (noise == null) return;
			int maxY = minY + noise.length();
			boolean previousCloud = false;
			IntList floorLevels = null, ceilingLevels = null;
			for (int y = minY; y < maxY; y++) {
				int index = y - minY;
				boolean currentCloud = noise.getF(index) > 0.0F;
				if (previousCloud && !currentCloud) {
					if (floorLevels == null) floorLevels = new IntArrayList(1);
					floorLevels.add(y);
				}
				else if (currentCloud && !previousCloud) {
					if (ceilingLevels == null) ceilingLevels = new IntArrayList(1);
					ceilingLevels.add(y - 1);
				}
				previousCloud = currentCloud;
			}
			this.floor.set(column, floorLevels);
			this.ceiling.set(column, ceilingLevels);
		}
	}

	public void updateLevels() {
		CloudLevelUpdater.NEST        .update(this, this.settings.nest.min_y());
		CloudLevelUpdater.LOWER_RING  .update(this, this.getLowerRingCloudSampleStartY());
		CloudLevelUpdater.UPPER_RING  .update(this, this.getUpperRingCloudSampleStartY());
		CloudLevelUpdater.LOWER_BRIDGE.update(this, this.getLowerBridgeCloudSampleStartY());
		CloudLevelUpdater.UPPER_BRIDGE.update(this, this.getUpperBridgeCloudSampleStartY());
	}

	//////////////////////////////// warp ////////////////////////////////

	public double getWarpX() {
		return (
			this.setFlag(WARP_X)
			? this.warpX = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				(EndColumn self) -> self.settings.warp_x.getValue(self.seed, self.x, self.z)
			)
			: this.warpX
		);
	}

	public double getWarpZ() {
		return (
			this.setFlag(WARP_Z)
			? this.warpZ = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				(EndColumn self) -> self.settings.warp_z.getValue(self.seed, self.x, self.z)
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

	public double getAngleToOrigin() {
		return this.setFlag(ANGLE_TO_ORIGIN) ? this.angleToOrigin = Math.atan2(this.z, this.x) : this.angleToOrigin;
	}

	public static String debug_distanceToOrigin(CustomDisplayContext context) {
		return CustomDisplayContext.format(context.<EndColumn>column().getDistanceToOrigin()) + " block(s) " + context.arrow(0, 0);
	}

	//////////////////////////////// nest ////////////////////////////////

	public @Nullable NumberArray getNestNoise() {
		if (this.getDistanceToOrigin() >= this.settings.nest.max_radius()) return null;
		NumberArray nestNoise = this.nestNoise;
		if (this.setFlag(NEST_NOISE)) {
			if (nestNoise == null) {
				nestNoise = this.nestNoise = NumberArray.allocateFloatsHeap(this.settings.nest.verticalSamples());
			}
			int startY = this.settings.nest.min_y();
			ScriptedGrid.SECRET_COLUMN.accept(this, nestNoise, (NumberArray noise) -> {
				this.settings.nest.shape().getBulkY(this.seed, this.x, startY, this.z, noise);
			});
		}
		return nestNoise;
	}

	public double getNestNoise(double y) {
		return this.getNestNoise(BigGlobeMath.floorI(y));
	}

	public double getNestNoise(int y) {
		return ScriptedGrid.SECRET_COLUMN.apply(this, (EndColumn self) -> {
			return self.settings.nest.shape().getValue(self.seed, self.x, y, self.z);
		});
	}

	//////////////////////////////// mountains ////////////////////////////////

	public double getMountainCenterY() {
		return (
			this.setFlag(MOUNTAIN_CENTER_Y)
			? this.mountainCenterY = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				(EndColumn self) -> self.settings.mountains.center_y().getValue(self.seed, self.x, self.z)
			)
			: this.mountainCenterY
		);
	}

	public double getMountainThickness() {
		return (
			this.setFlag(MOUNTAIN_THICKNESS)
			? this.mountainThickness = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				(EndColumn self) -> self.settings.mountains.thickness().evaluate(self, self.getMountainCenterY())
			)
			: this.mountainThickness
		);
	}

	public double getFoliage() {
		return (
			this.setFlag(FOLIAGE)
			? this.foliage = ScriptedGrid.SECRET_COLUMN.apply(
				this,
				(EndColumn self) -> self.settings.mountains.foliage().getValue(this.seed, this.x, this.z)
			)
			: this.foliage
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
		return this.setFlag(RING_CLOUD_HORIZONTAL_BIAS) ? this.ringCloudHorizontalBias = this.computeRingCloudHorizontalBias() : this.ringCloudHorizontalBias;
	}

	public double computeRingCloudHorizontalBias() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		double mid       = (ringCloudSettings.max_radius() + ringCloudSettings.min_radius()) * 0.5D;
		double halfRange = (ringCloudSettings.max_radius() - ringCloudSettings.min_radius()) * 0.5D;
		return BigGlobeMath.squareD((this.getWarpRadius() - mid) / halfRange);
	}

	public double getRingCloudRawNoise(double y) {
		return this.getRingCloudRawNoise(BigGlobeMath.floorI(y));
	}

	public double getRingCloudRawNoise(int y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		return ringCloudSettings.noise().getValue(this.seed, this.x, y, this.z);
	}

	//////////////// lower ////////////////

	public double getLowerRingCloudCenterY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		double mountainCenterY = this.getMountainCenterY();
		return mountainCenterY - ringCloudSettings.center_y().evaluate(this, mountainCenterY);
	}

	public double getLowerRingCloudVerticalBias(double y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		double centerY = this.getLowerRingCloudCenterY();
		return BigGlobeMath.squareD((y - centerY) / ringCloudSettings.vertical_thickness());
	}

	public double getLowerRingCloudBias(double y) {
		if (this.settings.ring_clouds == null) return Double.NaN;
		return this.getRingCloudHorizontalBias() + this.getLowerRingCloudVerticalBias(y);
	}

	public int getLowerRingCloudSampleStartY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getLowerRingCloudCenterY();
		return BigGlobeMath.ceilI(centerY - ringCloudSettings.vertical_thickness());
	}

	public int getLowerRingCloudSampleEndY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getLowerRingCloudCenterY();
		return BigGlobeMath.ceilI(centerY + ringCloudSettings.vertical_thickness());
	}

	public @Nullable NumberArray getLowerRingCloudNoise() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return null;
		double horizontalBias = this.getRingCloudHorizontalBias();
		if (horizontalBias <= -ringCloudSettings.noise().maxValue()) return null;
		NumberArray lowerRingCloudNoise = this.lowerRingCloudNoise;
		if (this.setFlag(LOWER_RING_CLOUD_NOISE)) {
			if (lowerRingCloudNoise == null) {
				lowerRingCloudNoise = this.lowerRingCloudNoise = NumberArray.allocateFloatsHeap(ringCloudSettings.verticalSamples());
			}
			int startY = this.getLowerRingCloudSampleStartY();
			ScriptedGrid.SECRET_COLUMN.accept(this, lowerRingCloudNoise, (NumberArray noise) -> {
				ringCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, noise);
			});
			double noiseMax = ringCloudSettings.noise().maxValue();
			for (int index = 0, length = lowerRingCloudNoise.length(); index < length; index++) {
				lowerRingCloudNoise.sub(index, (horizontalBias + this.getLowerRingCloudVerticalBias(index + startY)) * noiseMax);
			}
		}
		return lowerRingCloudNoise;
	}

	public double getLowerRingCloudBiasedNoise(double y) {
		return this.getLowerRingCloudBiasedNoise(BigGlobeMath.floorI(y));
	}

	public double getLowerRingCloudBiasedNoise(int y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		return (
			ringCloudSettings.noise().getValue(this.seed, this.x, y, this.z)
			- this.getLowerRingCloudBias(y)
			* ringCloudSettings.noise().maxValue()
		);
	}

	//////////////// upper ////////////////

	public double getUpperRingCloudCenterY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		double mountainCenterY = this.getMountainCenterY();
		return mountainCenterY + ringCloudSettings.center_y().evaluate(this, mountainCenterY);
	}

	public double getUpperRingCloudVerticalBias(double y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		double centerY = this.getUpperRingCloudCenterY();
		return BigGlobeMath.squareD((y - centerY) / ringCloudSettings.vertical_thickness());
	}

	public double getUpperRingCloudBias(double y) {
		if (this.settings.ring_clouds == null) return Double.NaN;
		return this.getRingCloudHorizontalBias() + this.getUpperRingCloudVerticalBias(y);
	}

	public int getUpperRingCloudSampleStartY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getUpperRingCloudCenterY();
		return BigGlobeMath.ceilI(centerY - ringCloudSettings.vertical_thickness());
	}

	public int getUpperRingCloudSampleEndY() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getUpperRingCloudCenterY();
		return BigGlobeMath.ceilI(centerY + ringCloudSettings.vertical_thickness());
	}

	public @Nullable NumberArray getUpperRingCloudNoise() {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return null;
		double horizontalBias = this.getRingCloudHorizontalBias();
		if (horizontalBias <= -ringCloudSettings.noise().maxValue()) return null;
		NumberArray upperRingCloudNoise = this.upperRingCloudNoise;
		if (this.setFlag(UPPER_RING_CLOUD_NOISE)) {
			if (upperRingCloudNoise == null) {
				upperRingCloudNoise = this.upperRingCloudNoise = NumberArray.allocateFloatsHeap(ringCloudSettings.verticalSamples());
			}
			int startY = this.getUpperRingCloudSampleStartY();
			ScriptedGrid.SECRET_COLUMN.accept(this, upperRingCloudNoise, (NumberArray noise) -> {
				ringCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, noise);
			});
			double noiseMax = ringCloudSettings.noise().maxValue();
			for (int index = 0, length = upperRingCloudNoise.length(); index < length; index++) {
				upperRingCloudNoise.sub(index, (horizontalBias + this.getUpperRingCloudVerticalBias(index + startY)) * noiseMax);
			}
		}
		return upperRingCloudNoise;
	}

	public double getUpperRingCloudBiasedNoise(double y) {
		return this.getUpperRingCloudBiasedNoise(BigGlobeMath.floorI(y));
	}

	public double getUpperRingCloudBiasedNoise(int y) {
		RingCloudSettings ringCloudSettings = this.settings.ring_clouds;
		if (ringCloudSettings == null) return Double.NaN;
		return (
			ringCloudSettings.noise().getValue(this.seed, this.x, y, this.z)
			- this.getUpperRingCloudBias(y)
			* ringCloudSettings.noise().maxValue()
		);
	}

	//////////////////////////////// bridge clouds ////////////////////////////////

	public double getBridgeCloudArchness() {
		return this.setFlag(BRIDGE_CLOUD_ARCHNESS) ? this.bridgeCloudArchness = this.computeBridgeCloudArchness() : this.bridgeCloudArchness;
	}

	public double computeBridgeCloudArchness() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		return bridgeCloudSettings.center_y().evaluate(this, this.getMountainCenterY());
	}

	public double getBridgeCloudRadialBias() {
		return this.setFlag(BRIDGE_CLOUD_RADIAL_BIAS) ? this.bridgeCloudRadialBias = this.computeBridgeCloudRadialBias() : this.bridgeCloudRadialBias;
	}

	public double computeBridgeCloudRadialBias() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		double radius = this.getWarpRadius();
		if (radius >= bridgeCloudSettings.mid_radius()) return 0.0D;
		double range = bridgeCloudSettings.mid_radius() - bridgeCloudSettings.min_radius();
		return BigGlobeMath.squareD((bridgeCloudSettings.mid_radius() - radius) / range);
	}

	public double getBridgeCloudAngularBias() {
		return this.setFlag(BRIDGE_CLOUD_ANGULAR_BIAS) ? this.bridgeCloudAngularBias = this.computeBridgeCloudAngularBias() : this.bridgeCloudAngularBias;
	}

	public double computeBridgeCloudAngularBias() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		double angle = this.getWarpAngle() * bridgeCloudSettings.count();
		angle += Permuter.nextPositiveDouble(this.seed ^ 0x39BF90041EA9E0D0L) * BigGlobeMath.TAU;
		return Math.sin(angle) * 0.5D + 0.5D;
	}

	public double getBridgeCloudHorizontalBias() {
		return this.getBridgeCloudRadialBias() + this.getBridgeCloudAngularBias();
	}

	public double getBridgeCloudRawNoise(double y) {
		return this.getBridgeCloudRawNoise(BigGlobeMath.floorI(y));
	}

	public double getBridgeCloudRawNoise(int y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		return bridgeCloudSettings.noise().getValue(this.seed, this.x, y, this.z);
	}

	//////////////// lower ////////////////

	public double getLowerBridgeCloudCenterY() {
		return this.getMountainCenterY() - this.getBridgeCloudArchness();
	}

	public double getLowerBridgeCloudVerticalBias(double y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		double centerY = this.getLowerBridgeCloudCenterY();
		return BigGlobeMath.squareD((y - centerY) / bridgeCloudSettings.vertical_thickness());
	}

	public double getLowerBridgeCloudBias(double y) {
		return this.getBridgeCloudHorizontalBias() + this.getLowerBridgeCloudVerticalBias(y);
	}

	public int getLowerBridgeCloudSampleStartY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getLowerBridgeCloudCenterY();
		return BigGlobeMath.ceilI(centerY - bridgeCloudSettings.vertical_thickness());
	}

	public int getLowerBridgeCloudSampleEndY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getLowerBridgeCloudCenterY();
		return BigGlobeMath.ceilI(centerY + bridgeCloudSettings.vertical_thickness());
	}

	public @Nullable NumberArray getLowerBridgeCloudNoise() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return null;
		double horizontalBias = this.getBridgeCloudHorizontalBias();
		if (horizontalBias >= 1.0D) return null;
		NumberArray lowerBridgeCloudNoise = this.lowerBridgeCloudNoise;
		if (this.setFlag(LOWER_BRIDGE_CLOUD_NOISE)) {
			if (lowerBridgeCloudNoise == null) {
				lowerBridgeCloudNoise = this.lowerBridgeCloudNoise = NumberArray.allocateFloatsHeap(bridgeCloudSettings.verticalSamples());
			}
			int startY = this.getLowerBridgeCloudSampleStartY();
			ScriptedGrid.SECRET_COLUMN.accept(this, lowerBridgeCloudNoise, (NumberArray noise) -> {
				bridgeCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, noise);
			});
			double noiseMax = bridgeCloudSettings.noise().maxValue();
			for (int index = 0, length = lowerBridgeCloudNoise.length(); index < length; index++) {
				lowerBridgeCloudNoise.sub(index, (horizontalBias + this.getLowerBridgeCloudVerticalBias(index + startY)) * noiseMax);
			}
		}
		return lowerBridgeCloudNoise;
	}

	public double getLowerBridgeCloudBiasedNoise(double y) {
		return this.getLowerBridgeCloudBiasedNoise(BigGlobeMath.floorI(y));
	}

	public double getLowerBridgeCloudBiasedNoise(int y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		return (
			bridgeCloudSettings.noise().getValue(this.seed, this.x, y, this.z)
			- this.getLowerBridgeCloudBias(y)
			* bridgeCloudSettings.noise().maxValue()
		);
	}

	//////////////// upper ////////////////

	public double getUpperBridgeCloudCenterY() {
		return this.getMountainCenterY() + this.getBridgeCloudArchness();
	}

	public double getUpperBridgeCloudVerticalBias(double y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		double centerY = this.getUpperBridgeCloudCenterY();
		return BigGlobeMath.squareD((y - centerY) / bridgeCloudSettings.vertical_thickness());
	}

	public double getUpperBridgeCloudBias(double y) {
		return this.getBridgeCloudHorizontalBias() + this.getUpperBridgeCloudVerticalBias(y);
	}

	public int getUpperBridgeCloudSampleStartY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getUpperBridgeCloudCenterY();
		return BigGlobeMath.ceilI(centerY - bridgeCloudSettings.vertical_thickness());
	}

	public int getUpperBridgeCloudSampleEndY() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Integer.MIN_VALUE;
		double centerY = this.getUpperBridgeCloudCenterY();
		return BigGlobeMath.ceilI(centerY + bridgeCloudSettings.vertical_thickness());
	}

	public @Nullable NumberArray getUpperBridgeCloudNoise() {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return null;
		double horizontalBias = this.getBridgeCloudHorizontalBias();
		if (horizontalBias >= 1.0D) return null;
		NumberArray upperBridgeCloudNoise = this.upperBridgeCloudNoise;
		if (this.setFlag(UPPER_BRIDGE_CLOUD_NOISE)) {
			if (upperBridgeCloudNoise == null) {
				upperBridgeCloudNoise = this.upperBridgeCloudNoise = NumberArray.allocateFloatsHeap(bridgeCloudSettings.verticalSamples());
			}
			int startY = this.getUpperBridgeCloudSampleStartY();
			ScriptedGrid.SECRET_COLUMN.accept(this, upperBridgeCloudNoise, (NumberArray noise) -> {
				bridgeCloudSettings.noise().getBulkY(this.seed, this.x, startY, this.z, noise);
			});
			double noiseMax = bridgeCloudSettings.noise().maxValue();
			for (int index = 0, length = upperBridgeCloudNoise.length(); index < length; index++) {
				upperBridgeCloudNoise.sub(index, (horizontalBias + this.getUpperBridgeCloudVerticalBias(index + startY)) * noiseMax);
			}
		}
		return upperBridgeCloudNoise;
	}

	public double getUpperBridgeCloudBiasedNoise(double y) {
		return this.getUpperBridgeCloudBiasedNoise(BigGlobeMath.floorI(y));
	}

	public double getUpperBridgeCloudBiasedNoise(int y) {
		BridgeCloudSettings bridgeCloudSettings = this.settings.bridge_clouds;
		if (bridgeCloudSettings == null) return Double.NaN;
		return (
			bridgeCloudSettings.noise().getValue(this.seed, this.x, y, this.z)
			- this.getUpperBridgeCloudBias(y)
			* bridgeCloudSettings.noise().maxValue()
		);
	}

	//////////////////////////////// misc ////////////////////////////////

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return this.settings.biomes.getBiome(this, y, this.seed);
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