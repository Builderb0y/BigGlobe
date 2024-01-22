package builderb0y.bigglobe.versions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;

import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.command.argument.BlockArgumentParser.TagResult;
import net.minecraft.registry.Registry;

#if MC_VERSION > MC_1_19_2
	import net.minecraft.registry.RegistryWrapper;
#endif
@Deprecated //minecraft will hopefully be making blocks data-driven in the future.
public class BlockArgumentParserVersions {

	#if MC_VERSION == MC_1_19_2
		public static Registry<Block> blockRegistry() {
			return RegistryVersions.block();
		}
	#else
		public static RegistryWrapper<Block> blockRegistry() {
			return RegistryVersions.block().getReadOnlyWrapper();
		}
	#endif

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