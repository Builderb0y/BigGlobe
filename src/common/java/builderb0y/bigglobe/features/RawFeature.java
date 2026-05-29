package builderb0y.bigglobe.features;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public interface RawFeature<T_Config extends FeatureConfiguration> {

	public boolean generate(WorldWrapper world, T_Config config, BlockPos pos);
}