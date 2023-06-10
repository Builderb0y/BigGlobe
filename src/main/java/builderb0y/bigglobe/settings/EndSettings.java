package builderb0y.bigglobe.settings;

import java.util.stream.Stream;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.scripting.ColumnYToDoubleScript;

public record EndSettings(
	@VerifyDivisibleBy16 int min_y,
	@VerifyDivisibleBy16 @VerifySorted(greaterThan = "min_y") int max_y,
	Grid2D warp_x,
	Grid2D warp_z,
	EndNestSettings nest,
	EndMountainSettings mountains,
	@VerifyNullable RingCloudSettings ring_clouds,
	@VerifyNullable BridgeCloudSettings bridge_clouds,
	EndBiomes biomes
) {

	public record EndNestSettings(
		Grid2D noise
	) {}

	public record EndMountainSettings(
		Grid2D center_y,
		ColumnYToDoubleScript.Holder thickness
	) {}

	public record RingCloudSettings(
		Grid3D noise,
		double center_y,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double min_radius,
		@VerifySorted(greaterThan = "min_radius") double max_radius,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness
	) {

		public int verticalSamples() {
			return BigGlobeMath.floorI(this.vertical_thickness * 2.0D) + 1;
		}
	}

	public record BridgeCloudSettings(
		Grid3D noise,
		double base_y,
		double archness,
		@VerifyIntRange(min = 0) int count,
		@VerifyFloatRange(min = 0.0D) double min_radius,
		@VerifySorted(greaterThan = "min_radius") double mid_radius,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness
	) {

		public int verticalSamples() {
			return BigGlobeMath.floorI(this.vertical_thickness * 2.0D) + 1;
		}
	}

	@Wrapper
	public static class EndBiomes {

		public final RegistryEntryLookup<Biome> biomes;
		public final RegistryEntry<Biome> the_end, the_void;

		public EndBiomes(RegistryEntryLookup<Biome> biomes) {
			this.biomes = biomes;
			this.the_end = biomes.getOrThrow(BiomeKeys.THE_END);
			this.the_void = biomes.getOrThrow(BiomeKeys.THE_VOID);
		}

		public Stream<RegistryEntry<Biome>> stream() {
			return Stream.of(this.the_end, this.the_void);
		}
	}
}