package builderb0y.bigglobe.versions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.command.argument.BlockArgumentParser.TagResult;
import net.minecraft.registry.RegistryWrapper;

@Deprecated //minecraft will hopefully be making blocks data-driven in the future.
public class BlockArgumentParserVersions {

	public static RegistryWrapper<Block> blockRegistry() {
		return RegistryVersions.block().getReadOnlyWrapper();
	}

	public static BlockResult block(String string, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.block(blockRegistry(), string, allowNBT);
	}

	public static BlockResult block(StringReader stringReader, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.block(blockRegistry(), stringReader, allowNBT);
	}

	public static Either<BlockResult, TagResult> blockOrTag(String string, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.blockOrTag(blockRegistry(), string, allowNBT);
	}

	public static Either<BlockResult, TagResult> blockOrTag(StringReader stringReader, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.blockOrTag(blockRegistry(), stringReader, allowNBT);
	}
}