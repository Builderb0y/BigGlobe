package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import org.apache.commons.lang3.mutable.MutableBoolean;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.versions.RegistryVersions;

public class EndSpikeWorldgenFeature extends Feature<NoneFeatureConfiguration> {

	public EndSpikeWorldgenFeature(Codec<NoneFeatureConfiguration> configCodec) {
		super(configCodec);
	}

	public EndSpikeWorldgenFeature() {
		this(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		ConfiguredFeature<?, ?> delegate = RegistryVersions.getObject(context.level().registryAccess(), EndSpikeRespawnFeature.DELEGATE_KEY);
		if (delegate == null) return false;
		MutableBoolean success = new MutableBoolean(false);
		EndSpikeFeature
		.getSpikesForLevel(context.level())
		.stream()
		.filter((EndSpike spike) -> spike.isCenterWithinChunk(context.origin()))
		.forEach((EndSpike spike) -> {
			if (
				delegate.place(
					context.level(),
					context.chunkGenerator(),
					new MojangPermuter(EndSpikeRespawnFeature.getRandomSeed(context.level(), spike.getCenterX(), spike.getCenterZ())),
					new BlockPos(
						spike.getCenterX(),
						spike.getHeight(),
						spike.getCenterZ()
					)
				)
			) {
				success.setTrue();
			}
		});
		return success.booleanValue();
	}
}