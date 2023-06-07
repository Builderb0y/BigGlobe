package builderb0y.bigglobe.settings;

import java.util.stream.Stream;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.codecs.VerifyDivisibleBy16;
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
	RingCloudSettings ring_clouds,
	BridgeCloudSettings bridge_clouds,
	EndBiomes biomes
) {

	public record EndNestSettings(
		Grid2D noise
	) {}

	public record EndMountainSettings(
		Grid2D noise,
		ColumnYToDoubleScript.Holder thickness
	) {}

	public record RingCloudSettings(
		Grid3D noise,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness
	) {}

	public record BridgeCloudSettings(
		Grid3D noise,
		@VerifyIntRange(min = 0) int count,
		@VerifyFloatRange(min = 0.0D, minInclusive = false) double vertical_thickness,
		double archness
	) {}

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