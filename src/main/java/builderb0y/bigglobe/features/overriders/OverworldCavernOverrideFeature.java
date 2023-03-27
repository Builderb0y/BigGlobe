package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.overworld.OverworldCavernOverrider;
import builderb0y.bigglobe.overriders.overworld.OverworldCavernOverrider.Holder;

public class OverworldCavernOverrideFeature extends DummyFeature<OverworldCavernOverrideFeature.Config> {

	public OverworldCavernOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public OverworldCavernOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final OverworldCavernOverrider.Holder script;

		public Config(Holder script) {
			this.script = script;
		}
	}
}