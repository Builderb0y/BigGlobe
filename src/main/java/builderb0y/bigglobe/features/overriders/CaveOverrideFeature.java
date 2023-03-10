package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldCaveOverrider.Holder;

public class CaveOverrideFeature extends DummyFeature<CaveOverrideFeature.Config> {

	public CaveOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public CaveOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldCaveOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}