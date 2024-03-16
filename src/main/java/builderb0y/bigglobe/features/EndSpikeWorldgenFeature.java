package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import org.apache.commons.lang3.mutable.MutableBoolean;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class EndSpikeWorldgenFeature extends Feature<DefaultFeatureConfig> {

	public EndSpikeWorldgenFeature(Codec<DefaultFeatureConfig> configCodec) {
		super(configCodec);
	}

	public EndSpikeWorldgenFeature() {
		this(DefaultFeatureConfig.CODEC);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		ConfiguredFeature<?, ?> delegate = context.getWorld().getRegistryManager().get(RegistryKeyVersions.configuredFeature()).get(EndSpikeRespawnFeature.DELEGATE_KEY);
		if (delegate == null) return false;
		MutableBoolean success = new MutableBoolean(false);
		EndSpikeFeature
		.getSpikes(context.getWorld())
		.stream()
		.filter((Spike spike) -> spike.isInChunk(context.getOrigin()))
		.forEach((Spike spike) -> {
			if (
				delegate.generate(
					context.getWorld(),
					context.getGenerator(),
					new MojangPermuter(EndSpikeRespawnFeature.getRandomSeed(context.getWorld(), spike.getCenterX(), spike.getCenterZ())),
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