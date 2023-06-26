package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldFoliageOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldFoliageOverrider.Holder;

public class OverworldFoliageOverrideFeature extends DummyFeature<OverworldFoliageOverrideFeature.Config> {

	public OverworldFoliageOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public OverworldFoliageOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldFoliageOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}