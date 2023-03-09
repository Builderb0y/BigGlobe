package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldFoliageOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldFoliageOverrider.Holder;

public class FoliageOverrideFeature extends DummyFeature<FoliageOverrideFeature.Config> {

	public FoliageOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public FoliageOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldFoliageOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}