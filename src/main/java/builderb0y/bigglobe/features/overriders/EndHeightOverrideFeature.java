package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.end.EndHeightOverrider;

public class EndHeightOverrideFeature extends DummyFeature<EndHeightOverrideFeature.Config> {

	public EndHeightOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public EndHeightOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final EndHeightOverrider.Holder script;

		public Config(EndHeightOverrider.Holder script) {
			this.script = script;
		}
	}
}