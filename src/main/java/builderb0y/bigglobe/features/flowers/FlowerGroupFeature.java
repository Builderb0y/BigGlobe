package builderb0y.bigglobe.features.flowers;

import com.mojang.serialization.Codec;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifySorted;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig.GroupConfig;
import builderb0y.bigglobe.randomSources.RandomSource;

public class FlowerGroupFeature extends DummyFeature<FlowerGroupFeature.Config> {

	public FlowerGroupFeature(Codec<Config> codec) {
		super(codec);
	}

	public FlowerGroupFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends GroupConfig {

		@VerifyIntRange(min = 0, minInclusive = false) final public int scale;
		@VerifyIntRange(min = 0) @VerifySorted(lessThanOrEqual = "scale") public final int variation;
		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double spawn_chance;
		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double randomize_chance;
		public final RandomSource randomize_radius;

		public Config(
			int scale,
			int variation,
			double spawn_chance,
			double randomize_chance,
			RandomSource randomize_radius
		) {
			this.scale            = scale;
			this.variation        = variation;
			this.spawn_chance     = spawn_chance;
			this.randomize_chance = randomize_chance;
			this.randomize_radius = randomize_radius;
		}
	}
}