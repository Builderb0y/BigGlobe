package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.features.LinkedConfig.GroupConfig;

public class RockLayerGroupFeature extends DummyFeature<RockLayerGroupFeature.Config> {

	public RockLayerGroupFeature(Codec<Config> codec) {
		super(codec);
	}

	public RockLayerGroupFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends GroupConfig {

		public final @VerifyFloatRange(min = 0.0D, minInclusive = false) double repeat;

		public Config(double repeat) {
			this.repeat = repeat;
		}
	}
}