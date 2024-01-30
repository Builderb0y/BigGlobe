package builderb0y.bigglobe.features;

import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public interface RawFeature<T_Config> {

	public boolean generate(WorldWrapper world, T_Config config, BlockPos pos);
}