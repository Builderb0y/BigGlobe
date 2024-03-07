package builderb0y.bigglobe.features;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.FeatureConfig;

import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public interface RawFeature<T_Config extends FeatureConfig> {

	public boolean generate(WorldWrapper world, T_Config config, BlockPos pos);
}