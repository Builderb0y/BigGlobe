package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

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
	public boolean generate(FeatureContext<Config> context) {
		return (
			context
			.getConfig()
			.choices
			.getRandomElement(Permuter.from(context.getRandom()))
			.value()
			.generate(
				context.getWorld(),
				context.getGenerator(),
				context.getRandom(),
				context.getOrigin()
			)
		);
	}

	public static record Config(
		@VerifyNotEmpty RandomList<
			@UseName("feature") RegistryEntry<
				ConfiguredFeature<?, ?>
			>
		>
		choices
	)
	implements FeatureConfig {}
}