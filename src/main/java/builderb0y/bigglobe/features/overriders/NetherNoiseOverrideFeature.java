package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.nether.NoiseOverrider;
import builderb0y.bigglobe.overriders.nether.NoiseOverrider.Holder;

public class NetherNoiseOverrideFeature extends DummyFeature<NetherNoiseOverrideFeature.Config> {

	public NetherNoiseOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public NetherNoiseOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final NoiseOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}