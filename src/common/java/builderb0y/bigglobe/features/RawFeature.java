package builderb0y.bigglobe.features;

import net.minecraft.core.BlockPos;

import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;

public interface RawFeature<T_Config extends FeatureAdapter.Config> {

	#if MC_VERSION >= MC_26_3_0

		public abstract T_Config config();

	#endif

	public abstract boolean generate(WorldWrapper world, T_Config config, BlockPos pos);
}