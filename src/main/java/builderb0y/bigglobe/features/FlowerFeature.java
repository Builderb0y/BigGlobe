package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.randomLists.IRestrictedListElement;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.settings.Seed.SeedModes;
import builderb0y.bigglobe.settings.VariationsList;

public class FlowerFeature extends DummyFeature<FlowerFeature.Config> {

	public FlowerFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public FlowerFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final @SeedModes(Seed.NUMBER | Seed.STRING) Seed seed;
		public final @VerifyIntRange(min = 0, minInclusive = false) int distance;
		public final @VerifyIntRange(min = 0) @VerifySorted(lessThanOrEqual = "distance") int variation;
		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double spawn_chance;
		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double randomize_chance;
		public final RandomSource randomize_radius;
		public final Grid2D noise;
		public final VariationsList<Entry> entries;

		public Config(
			Seed seed,
			int distance,
			int variation,
			double spawn_chance,
			double randomize_chance,
			RandomSource randomize_radius,
			Grid2D noise,
			VariationsList<Entry> entries
		) {
			this.seed = seed;
			this.distance = distance;
			this.variation = variation;
			this.spawn_chance = spawn_chance;
			this.randomize_chance = randomize_chance;
			this.randomize_radius = randomize_radius;
			this.noise = noise;
			this.entries = entries;
		}
	}

	public static record Entry(
		double weight,
		ColumnRestriction restrictions,
		RandomSource radius,
		SingleBlockFeature.Config state,
		SingleBlockFeature.@VerifyNullable Config under
	)
	implements IRestrictedListElement {

		@Override
		public double getWeight() {
			return this.weight;
		}

		@Override
		public ColumnRestriction getRestrictions() {
			return this.restrictions;
		}
	}
}