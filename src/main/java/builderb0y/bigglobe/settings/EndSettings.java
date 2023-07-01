package builderb0y.bigglobe.settings;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.features.SortedFeatureTag;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;
import builderb0y.bigglobe.scripting.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.settings.BiomeLayout.EndBiomeLayout;

public record EndSettings(
	@VerifyDivisibleBy16 int min_y,
	@VerifyDivisibleBy16 @VerifySorted(greaterThan = "min_y") int max_y,
	Grid2D warp_x,
	Grid2D warp_z,
	EndNestSettings nest,
	EndMountainSettings mountains,
	@VerifyNullable RingCloudSettings ring_clouds,
	@VerifyNullable BridgeCloudSettings bridge_clouds,
	BiomeLayout.Holder<EndBiomeLayout> biomes
) {

	public record EndNestSettings(
		Grid3D shape,
		int min_y,
		int max_y,
		int max_radius,
		int @VerifySizeRange(min = 3, max = 3) [] spawn_location,
		boolean spawn_obsidian_platform,
		@VerifyFloatRange(min = 0.0D) double gateway_radius,
		int gateway_height,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator
	) {

		public int verticalSamples() {
			return this.max_y - this.min_y + 1;
		}
	}

	public record EndMountainSettings(
		Grid2D center_y,
		ColumnYToDoubleScript.Holder thickness,
		Grid2D foliage,
		SortedFeatureTag floor_decorator,
		SortedFeatureTag ceiling_decorator,
		SurfaceDepthWithSlopeScript.Holder primary_surface_depth
	) {}

	public static interface EndCloudSettings {

		public abstract Grid3D noise();

		public abstract ColumnYToDoubleScript.Holder center_y();

		public abstract double vertical_thickness();

		public default int verticalSamples() {
			return BigGlobeMath.floorI(this.vertical_thickness() * 2.0D) + 1;
		}

		public abstract SortedFeatureTag lower_floor_decorator();

		public abstract SortedFeatureTag lower_ceiling_decorator();

		public abstract SortedFeatureTag upper_floor_decorator();

		public abstract SortedFeatureTag upper_ceiling_decorator();
	}

	public record RingCloudSettings(
		Grid3D noise,
		ColumnYToDoubleScript.Holder center_y,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double min_radius,
		@VerifySorted(greaterThan = "min_radius") double max_radius,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness,
		SortedFeatureTag lower_floor_decorator,
		SortedFeatureTag lower_ceiling_decorator,
		SortedFeatureTag upper_floor_decorator,
		SortedFeatureTag upper_ceiling_decorator
	)
	implements EndCloudSettings {}

	public record BridgeCloudSettings(
		Grid3D noise,
		@VerifyIntRange(min = 0) int count,
		ColumnYToDoubleScript.Holder center_y,
		@VerifyFloatRange(min = 0.0D) double min_radius,
		@VerifySorted(greaterThan = "min_radius") double mid_radius,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness,
		SortedFeatureTag lower_floor_decorator,
		SortedFeatureTag lower_ceiling_decorator,
		SortedFeatureTag upper_floor_decorator,
		SortedFeatureTag upper_ceiling_decorator
	)
	implements EndCloudSettings {}
}