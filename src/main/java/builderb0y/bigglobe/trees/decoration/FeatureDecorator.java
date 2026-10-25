package builderb0y.bigglobe.trees.decoration;

import builderb0y.bigglobe.trees.TreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class FeatureDecorator implements BlockDecorator {

	public final Holder<ConfiguredFeature<?, ?>> feature;

	public FeatureDecorator(Holder<ConfiguredFeature<?, ?>> feature) {
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