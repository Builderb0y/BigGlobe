package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

public class PostFeatureDecorator implements TrunkDecorator {

	public final Holder<ConfiguredFeature<?, ?>> feature;

	public PostFeatureDecorator(Holder<ConfiguredFeature<?, ?>> feature) {
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