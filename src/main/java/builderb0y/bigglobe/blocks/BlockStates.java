package builderb0y.bigglobe.blocks;

import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.state.property.Property;

import builderb0y.bigglobe.versions.BlockArgumentParserVersions;

/** frequently used BlockState's. */
public class BlockStates {

	public static final BlockState
		AIR                    = Blocks.AIR.getDefaultState(),
		BEDROCK                = Blocks.BEDROCK.getDefaultState(),
		STONE                  = Blocks.STONE.getDefaultState(),
		COBBLESTONE            = Blocks.COBBLESTONE.getDefaultState(),
		FLOATSTONE             = BigGlobeBlocks.FLOATSTONE.getDefaultState(),
		DEEPSLATE              = Blocks.DEEPSLATE.getDefaultState(),
		COBBLED_DEEPSLATE      = Blocks.COBBLED_DEEPSLATE.getDefaultState(),
		DIRT                   = Blocks.DIRT.getDefaultState(),
		GRASS_BLOCK            = Blocks.GRASS_BLOCK.getDefaultState(),
		SAND                   = Blocks.SAND.getDefaultState(),
		OVERGROWN_SAND         = BigGlobeBlocks.OVERGROWN_SAND.getDefaultState(),
		SANDSTONE              = Blocks.SANDSTONE.getDefaultState(),
		CUT_SANDSTONE          = Blocks.CUT_SANDSTONE.getDefaultState(),
		CHISELED_SANDSTONE     = Blocks.CHISELED_SANDSTONE.getDefaultState(),
		SNOW                   = Blocks.SNOW.getDefaultState(),
		WATER                  = Blocks.WATER.getDefaultState(),
		LAVA                   = Blocks.LAVA.getDefaultState(),

		NETHERRACK             = Blocks.NETHERRACK.getDefaultState(),
		NETHER_BRICKS          = Blocks.NETHER_BRICKS.getDefaultState(),
		CRACKED_NETHER_BRICKS  = Blocks.CRACKED_NETHER_BRICKS.getDefaultState(),
		CHISELED_NETHER_BRICKS = Blocks.CHISELED_NETHER_BRICKS.getDefaultState(),
		RED_NETHER_BRICKS      = Blocks.RED_NETHER_BRICKS.getDefaultState(),
		OBSIDIAN               = Blocks.OBSIDIAN.getDefaultState(),
		CRYING_OBSIDIAN        = Blocks.CRYING_OBSIDIAN.getDefaultState(),
		SOUL_SAND              = Blocks.SOUL_SAND.getDefaultState(),
		SOUL_SOIL              = Blocks.SOUL_SOIL.getDefaultState(),

		END_STONE              = Blocks.END_STONE.getDefaultState(),

		DELAYED_GENERATION     = BigGlobeBlocks.DELAYED_GENERATION.getDefaultState();

	/**
	the syntax for {@link BlockState#with(Property, Comparable)}
	is excessively verbose for what it does.
	so, have this method here to use command syntax instead.
	this method is slow to parse states, so it is
	recommended to cache the parsed state after parsing.
	for example, in a static final field.
	it is NOT recommended to call this method from
	other methods besides static class initializers.
	*/
	public static BlockState of(String name) {
		try {
			BlockResult result = BlockArgumentParserVersions.block(name, false);
			Set<Property<?>> remaining = new HashSet<>(result.blockState().getProperties());
			remaining.removeAll(result.properties().keySet());
			if (!remaining.isEmpty()) {
				throw new IllegalArgumentException("Missing properties for state " + name + ": " + remaining);
			}
			return result.blockState();
		}
		catch (CommandSyntaxException e) {
			throw new IllegalArgumentException("Invalid block specifier: " + name, e);
		}
	}
}