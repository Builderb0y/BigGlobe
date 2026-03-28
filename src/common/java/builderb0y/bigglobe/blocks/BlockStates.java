package builderb0y.bigglobe.blocks;

import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.codecs.BlockStateCoder.Result;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
frequently used BlockState's.
*/
public class BlockStates {

	public static final BlockState
		AIR = Blocks.AIR.defaultBlockState(),
		VOID_AIR = Blocks.VOID_AIR.defaultBlockState(),
		STONE = Blocks.STONE.defaultBlockState(),
		SAND = Blocks.SAND.defaultBlockState(),
		SANDSTONE = Blocks.SANDSTONE.defaultBlockState(),
		CUT_SANDSTONE = Blocks.CUT_SANDSTONE.defaultBlockState(),
		CHISELED_SANDSTONE = Blocks.CHISELED_SANDSTONE.defaultBlockState(),
		SNOW = Blocks.SNOW.defaultBlockState(),
		WATER = Blocks.WATER.defaultBlockState(),
		LAVA = Blocks.LAVA.defaultBlockState(),

	NETHERRACK = Blocks.NETHERRACK.defaultBlockState(),
		NETHER_BRICKS = Blocks.NETHER_BRICKS.defaultBlockState(),
		CRACKED_NETHER_BRICKS = Blocks.CRACKED_NETHER_BRICKS.defaultBlockState(),
		CHISELED_NETHER_BRICKS = Blocks.CHISELED_NETHER_BRICKS.defaultBlockState(),
		RED_NETHER_BRICKS = Blocks.RED_NETHER_BRICKS.defaultBlockState(),
		OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState(),
		CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState(),
		SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState(),

	DELAYED_GENERATION = BigGlobeBlocks.DELAYED_GENERATION.defaultBlockState();

	/**
	the syntax for {@link BlockState#setValue(Property, Comparable)}
	is excessively verbose for what it does.
	so, have this method here to use command syntax instead.
	this method is slow to parse states, so it is
	recommended to cache the parsed state after parsing.
	for example, in a static final field.
	it is NOT recommended to call this method from
	other methods besides static class initializers.
	*/
	public static BlockState of(String name) {
		Result<BlockProperties> result = BlockStateCoder.decodeStateWithMissingErrors(new BetterHardCodedRegistry<>(BuiltInRegistries.BLOCK), name);
		if (result.errors() != null) throw new IllegalArgumentException(result.collectErrorsEager());
		return result.value().state();
	}
}