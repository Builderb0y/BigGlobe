package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.trees.TreeGenerator;

@UseCoder(name = "REGISTRY", usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface BlockDecorator extends CoderRegistryTyped {

	public static final CoderRegistry<BlockDecorator> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("tree_block_decorators"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("random_walk_leaves"), RandomWalkLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("scatter_leaves"    ),    ScatterLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("drooping_leaves"   ),   DroopingLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("adjacent_leaf"     ),   AdjacentLeafDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("stubby_branch"     ),   StubbyBranchDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("snow"              ),           SnowDecorator.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("feature"           ),        FeatureDecorator.class);
	}};

	public abstract void decorate(TreeGenerator generator, BlockPos pos, BlockState state);

	/**
	the BlockDecorator is deserialized from {@link #REGISTRY},
	which is a problem if the decorator has an internal state,
	since the same instance of it will be re-used over and over again,
	instead of a new instance being created for every tree.
	I didn't feel like making a dedicated BlockDecoratorFactory
	interface to create new instances when, at the time of writing this,
	only one implementation has an internal state to begin with.
	so instead I have this method to copy decorators with internal
	states before adding them to the {@link DecoratorConfig}.
	*/
	public default BlockDecorator copyIfMutable() {
		return this;
	}
}