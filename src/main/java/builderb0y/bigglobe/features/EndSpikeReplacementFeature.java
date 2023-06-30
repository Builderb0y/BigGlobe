package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.EndSpikeFeature.Spike;
import net.minecraft.world.gen.feature.EndSpikeFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class EndSpikeReplacementFeature extends Feature<EndSpikeFeatureConfig> {

	public static final RegistryKey<ConfiguredFeature<?, ?>> DELEGATE_KEY = RegistryKey.of(RegistryKeyVersions.configuredFeature(), BigGlobeMod.modID("end/nest_spike"));

	public EndSpikeReplacementFeature(Codec<EndSpikeFeatureConfig> configCodec) {
		super(configCodec);
	}

	public EndSpikeReplacementFeature() {
		this(EndSpikeFeatureConfig.CODEC);
	}

	public static long getRandomSeed(StructureWorldAccess world, int x, int z) {
		return Permuter.permute(world.getSeed() ^ 0x48FA509DA5C2D42DL, x, z);
	}

	@Override
	public boolean generate(FeatureContext<EndSpikeFeatureConfig> context) {
		ConfiguredFeature<?, ?> delegate = context.getWorld().getRegistryManager().get(RegistryKeyVersions.configuredFeature()).get(DELEGATE_KEY);
		if (delegate == null) return false;
		boolean placedAny = false;
		for (Spike spike : context.getConfig().getSpikes()) {
			long seed = getRandomSeed(context.getWorld(), spike.getCenterX(), spike.getCenterZ());
			BlockPos pos = new BlockPos(spike.getCenterX(), spike.getHeight(), spike.getCenterZ());
			//noinspection NonShortCircuitBooleanExpression
			placedAny |= delegate.generate(context.getWorld(), context.getGenerator(), new MojangPermuter(seed), pos);
		}
		return placedAny;
	}
}