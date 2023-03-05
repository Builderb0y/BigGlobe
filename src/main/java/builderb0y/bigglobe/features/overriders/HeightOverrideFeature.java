package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.DataOverworldHeightOverrider;
import builderb0y.bigglobe.overriders.overworld.DataOverworldHeightOverrider.Holder;

public class HeightOverrideFeature extends DummyFeature<HeightOverrideFeature.Config> {

	public HeightOverrideFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public HeightOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final DataOverworldHeightOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}