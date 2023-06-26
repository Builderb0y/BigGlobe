package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldHeightOverrider;

public class OverworldHeightOverrideFeature extends DummyFeature<OverworldHeightOverrideFeature.Config> {

	public OverworldHeightOverrideFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public OverworldHeightOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldHeightOverrider.Holder script;

		public Config(OverworldHeightOverrider.Holder script) {
			this.script = script;
		}
	}
}