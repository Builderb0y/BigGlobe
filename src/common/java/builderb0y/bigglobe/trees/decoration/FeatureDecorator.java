package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.trees.TreeGenerator;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class FeatureDecorator implements BlockDecorator {

	public final Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> feature;

	public FeatureDecorator(Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> feature) {
		this.feature = feature;
	}

	@Override
	public void decorate(TreeGenerator generator, BlockPos pos, BlockState state) {
		this.feature.value().place(
			generator.worldQueue,
			((ServerChunkCache)(generator.worldQueue.getChunkSource())).getGenerator(),
			generator.random.mojang(),
			pos
		);
	}
}