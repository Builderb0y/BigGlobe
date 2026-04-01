package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;

public class BigGlobeRandomFeature extends Feature<BigGlobeRandomFeature.Config> {

	public BigGlobeRandomFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public BigGlobeRandomFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		return (
			context
				.config()
				.choices
				.getRandomElement(Permuter.from(context.random()))
				.value()
				.place(
					context.level(),
					context.chunkGenerator(),
					context.random(),
					context.origin()
				)
		);
	}

	public static record Config(
		@VerifyNotEmpty RandomList<
									@UseName("feature") Holder<
																		ConfiguredFeature<?, ?>
																		>
									>
		choices
	)
		implements FeatureConfiguration {}
}