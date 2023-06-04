package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldSkylandOverrider;

public class OverworldSkylandOverrideFeature extends DummyFeature<OverworldSkylandOverrideFeature.Config> {

	public OverworldSkylandOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public OverworldSkylandOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldSkylandOverrider.Holder script;

		public Config(OverworldSkylandOverrider.Holder script) {
			this.script = script;
		}
	}
}