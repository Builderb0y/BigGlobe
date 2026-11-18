package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.levelgen.feature.Feature;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

#if MC_VERSION < MC_26_3_0
	import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
#endif

public class PostFeatureDecorator implements TrunkDecorator {

	public final Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> feature;

	public PostFeatureDecorator(Holder<#if MC_VERSION >= MC_26_3_0 Feature #else ConfiguredFeature<?, ?> #endif> feature) {
		this.feature = feature;
	}

	@Override
	public void decorate(TreeGenerator generator, TrunkConfig trunk) {
		this.feature.value().place(
			generator.worldQueue,
			((ServerChunkCache)(generator.worldQueue.getChunkSource())).getGenerator(),
			generator.random.mojang(),
			new BlockPos(
				BigGlobeMath.roundI(trunk.startX),
				trunk.startY,
				BigGlobeMath.roundI(trunk.startZ)
			)
		);
	}
}