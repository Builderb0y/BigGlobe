package builderb0y.bigglobe.features.overriders;

import com.mojang.serialization.Codec;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.features.DummyFeature;
import builderb0y.bigglobe.overriders.ScriptStructureOverrider;

public class StructureOverrideFeature extends DummyFeature<StructureOverrideFeature.Config> {

	public StructureOverrideFeature(Codec<Config> codec) {
		super(codec);
	}

	public StructureOverrideFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public static class Config extends DummyConfig {

		public final ScriptStructureOverrider.Holder script;

		public Config(ScriptStructureOverrider.Holder script) {
			this.script = script;
		}
	}
}