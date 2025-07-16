package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.tags.BlockTag;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockWrapper {

	public static final TypeInfo TYPE = type(Block.class);
	public static final MethodInfo
		GET_DEFAULT_STATE = MethodInfo.getMethod(BlockWrapper.class, "getDefaultState"),
		GET_DEFAULT_STATE_NULLABLE = MethodInfo.getMethod(BlockWrapper.class, "getDefaultStateNullable");
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockWrapper.class, "getBlock", String.class, Block.class);

	public static Block getBlock(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return getBlock(id, flags);
	}

	public static Block getBlock(String id, int flags) {
		if (id == null) return null;
		try {
			Block block = BlockStateCoder.blockOnly(BlockStateCoder.findBlockRegistry(), id);
			if (!BlockStateCoder.isEnabled(block)) {
				throw new RuntimeException("Disabled block: " + id);
			}
			return block;
		}
		catch (RuntimeException exception) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	@SuppressWarnings("deprecation")
	public static String id(Block block) {
		return UnregisteredObjectException.getID(block.getRegistryEntry()).toString();
	}

	public static boolean isIn(Block block, BlockTag tag) {
		return tag.contains(block);
	}

	public static BlockState getDefaultStateNullable(Block block) {
		return block == null ? null : block.getDefaultState();
	}

	public static BlockState getDefaultState(Block block) {
		return block.getDefaultState();
	}

	public static BlockState getRandomState(Block block, RandomGenerator random) {
		ImmutableList<BlockState> states = block.getStateManager().getStates();
		return states.get(random.nextInt(states.size()));
	}

	public static BlockState getRandomState(Block block, long seed) {
		ImmutableList<BlockState> states = block.getStateManager().getStates();
		return states.get(Permuter.nextBoundedInt(seed, states.size()));
	}
}