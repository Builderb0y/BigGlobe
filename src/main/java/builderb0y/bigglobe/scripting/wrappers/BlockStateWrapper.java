package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateWrapper {

	public static final TypeInfo TYPE = type(BlockState.class);
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockStateWrapper.class, "getState", String.class, BlockState.class);

	public static BlockState getState(MethodHandles.Lookup caller, String name, Class<?> type, String id) throws CommandSyntaxException {
		BlockResult result = BlockArgumentParser.block(BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.BLOCK).getReadOnlyWrapper(), id, false);
		Set<Property<?>> remaining = new HashSet<>(result.blockState().getProperties());
		remaining.removeAll(result.properties().keySet());
		if (!remaining.isEmpty()) {
			ScriptLogger.LOGGER.warn("Missing properties for state " + id + ": " + remaining);
		}
		return result.blockState();
	}

	public static BlockState getState(String id) throws CommandSyntaxException {
		//this method will be called only if the string is non-constant.
		//for performance reasons, we will skip properties checking here.
		return BlockArgumentParser.block(BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeys.BLOCK).getReadOnlyWrapper(), id, false).blockState();
	}

	public static boolean isIn(BlockState state, BlockTagKey key) {
		return state.isIn(key.key());
	}

	public static Block getBlock(BlockState state) {
		return state.getBlock();
	}

	public static boolean isAir(BlockState state) {
		return state.isAir();
	}

	public static boolean isReplaceable(BlockState state) {
		return state.getMaterial().isReplaceable();
	}

	public static boolean blocksLight(BlockState state) {
		return state.isOpaque();
	}

	public static boolean hasCollision(BlockState state) {
		return !state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty();
	}

	public static boolean hasFullCubeCollision(BlockState state) {
		return Block.isShapeFullCube(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
	}

	public static boolean hasFullCubeOutline(BlockState state) {
		return Block.isShapeFullCube(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
	}

	public static BlockState rotate(BlockState state, int rotation) {
		return switch (rotation) {
			case  90 -> state.rotate(BlockRotation.CLOCKWISE_90);
			case 180 -> state.rotate(BlockRotation.CLOCKWISE_180);
			case 270 -> state.rotate(BlockRotation.COUNTERCLOCKWISE_90);
			default  -> state;
		};
	}

	public static BlockState mirror(BlockState state, String axis) {
		if (axis.length() == 1) {
			char c = axis.charAt(0);
			if (c == 'x') return state.mirror(BlockMirror.FRONT_BACK);
			if (c == 'z') return state.mirror(BlockMirror.LEFT_RIGHT);
		}
		return state;
	}

	public static @Nullable Comparable<?> getProperty(BlockState state, String name) {
		Property<?> property = state.getBlock().getStateManager().getProperty(name);
		if (property == null) return null;
		Comparable<?> value = state.get(property);
		if (value instanceof StringIdentifiable e) {
			value = e.asString();
		}
		return value;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState with(BlockState state, String name, Comparable<?> value) {
		Property<?> property = state.getBlock().getStateManager().getProperty(name);
		if (property == null) return state;
		if (value instanceof String string) {
			value = property.parse(string).orElse(null);
			if (value == null) return state;
		}
		if (!property.getType().isInstance(value)) return state;
		return state.with((Property)(property), (Comparable)(value));
	}

	public static boolean canPlaceAt(WorldWrapper world, BlockState state, int x, int y, int z) {
		return world.getBlockState(x, y, z).getMaterial().isReplaceable() && state.canPlaceAt(world.world, world.pos.set(x, y, z));
	}

	public static boolean hasWater(BlockState state) {
		return state.getFluidState().isIn(FluidTags.WATER);
	}

	public static boolean hasLava(BlockState state) {
		return state.getFluidState().isIn(FluidTags.LAVA);
	}
}