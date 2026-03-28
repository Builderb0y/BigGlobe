package builderb0y.bigglobe.trees.decoration;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.trees.TreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface BlockDecorator extends CoderRegistryTyped<BlockDecorator> {

	public static final CoderRegistry<BlockDecorator> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("tree_block_decorators"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("random_walk_leaves"), RandomWalkLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("scatter_leaves"), ScatterLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("drooping_leaves"), DroopingLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("adjacent_leaf"), AdjacentLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("stubby_branch"), StubbyBranchDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("snow"), SnowDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("feature"), FeatureDecorator.class);
	}};

	public abstract void decorate(TreeGenerator generator, BlockPos pos, BlockState state);
}