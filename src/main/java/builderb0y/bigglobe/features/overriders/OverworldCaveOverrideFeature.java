package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider.Holder;

public class OverworldCaveOverrideFeature extends DummyFeature<OverworldCaveOverrideFeature.Config> {

	public OverworldCaveOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public OverworldCaveOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldCaveOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}