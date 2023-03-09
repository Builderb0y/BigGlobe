package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockWrapper {

	public static final TypeInfo TYPE = type(Block.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockWrapper.class, "getBlock", String.class, Block.class);

	public static Block getBlock(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getBlock(id);
	}

	public static Block getBlock(String id) {
		Identifier identifier = new Identifier(id);
		if (Registry.BLOCK.containsId(identifier)) {
			return Registry.BLOCK.get(identifier);
		}
		else {
			throw new IllegalArgumentException("Unknown block: " + id);
		}
	}

	@SuppressWarnings("deprecation")
	public static boolean isIn(Block block, BlockTagKey key) {
		return block.getRegistryEntry().isIn(key.key());
	}

	public static BlockState getDefaultState(Block block) {
		return block.getDefaultState();
	}
}