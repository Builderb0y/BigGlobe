package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DefineScriptTemplateFeature extends Feature<DefineScriptTemplateFeature.Config> {

	public DefineScriptTemplateFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public DefineScriptTemplateFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		return false;
	}

	public static record Config(@MultiLine String script, String[] inputs) implements FeatureConfig {}
}