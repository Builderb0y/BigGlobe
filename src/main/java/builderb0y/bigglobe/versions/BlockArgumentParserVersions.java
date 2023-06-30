package builderb0y.bigglobe.versions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.command.argument.BlockArgumentParser.TagResult;

public class BlockArgumentParserVersions {

	public static BlockResult block(String string, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.block(RegistryVersions.block().getReadOnlyWrapper(), string, allowNBT);
	}

	public static BlockResult block(StringReader stringReader, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.block(RegistryVersions.block().getReadOnlyWrapper(), stringReader, allowNBT);
	}

	public static Either<BlockResult, TagResult> blockOrTag(String string, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.blockOrTag(RegistryVersions.block().getReadOnlyWrapper(), string, allowNBT);
	}

	public static Either<BlockResult, TagResult> blockOrTag(StringReader stringReader, boolean allowNBT) throws CommandSyntaxException {
		return BlockArgumentParser.blockOrTag(RegistryVersions.block().getReadOnlyWrapper(), stringReader, allowNBT);
	}
}