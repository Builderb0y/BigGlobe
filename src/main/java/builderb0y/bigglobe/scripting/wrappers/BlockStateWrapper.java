package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

import builderb0y.bigglobe.fluids.BigGlobeFluidTags;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.BlockArgumentParserVersions;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.bigglobe.versions.TagsVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateWrapper {

	public static final TypeInfo TYPE = type(BlockState.class);
	public static final ConstantFactory
		CONSTANT_FACTORY = new ConstantFactory(BlockStateWrapper.class, "getState", String.class, BlockState.class),
		DEFAULT_CONSTANT_FACTORY = new ConstantFactory(BlockStateWrapper.class, "getDefaultState", String.class, BlockState.class);
	public static final MethodInfo
		GET_PROPERTY = MethodInfo.getMethod(BlockStateWrapper.class, "getProperty"),
		WITH = MethodInfo.getMethod(BlockStateWrapper.class, "with");

	public static BlockState getState(MethodHandles.Lookup caller, String name, Class<?> type, String id) throws CommandSyntaxException {
		if (id == null) return null;
		BlockResult result = BlockArgumentParserVersions.block(id, false);
		if (result.properties().size() != result.blockState().getProperties().size()) {
			Set<Property<?>> remaining = new HashSet<>(result.blockState().getProperties());
			remaining.removeAll(result.properties().keySet());
			if (!remaining.isEmpty()) {
				ScriptLogger.LOGGER.warn("Missing properties for state " + id + ": " + remaining);
			}
		}
		return result.blockState();
	}

	public static BlockState getState(String id) throws CommandSyntaxException {
		if (id == null) return null;
		//this method will be called only if the string is non-constant.
		//for performance reasons, we will skip properties checking here.
		return BlockArgumentParserVersions.block(id, false).blockState();
	}

	public static BlockState getDefaultState(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getDefaultState(id);
	}

	public static BlockState getDefaultState(String id) {
		if (id == null) return null;
		Identifier identifier = new Identifier(id);
		if (RegistryVersions.block().containsId(identifier)) {
			return RegistryVersions.block().get(identifier).getDefaultState();
		}
		else {
			throw new RuntimeException("Unknown block: " + id);
		}
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
		return BlockStateVersions.isReplaceable(state);
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
		return state.rotate(Directions.scriptRotation(rotation));
	}

	public static BlockState mirror(BlockState state, String axis) {
		return state.mirror(Directions.scriptMirror(axis));
	}

	@SuppressWarnings("unchecked")
	public static <C extends Comparable<C>> @Nullable C getProperty(BlockState state, String name) {
		Property<?> property = state.getBlock().getStateManager().getProperty(name);
		if (property == null) return null;
		Comparable<?> value = state.get(property);
		if (value instanceof StringIdentifiable e) {
			value = e.asString();
		}
		return (C)(value);
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
		BlockPos pos = world.immutablePos(x, y, z);
		return pos != null && BlockStateVersions.isReplaceable(world.world.getBlockState(pos)) && world.world.canPlace(pos, state);
	}

	public static boolean canStayAt(WorldWrapper world, BlockState state, int x, int y, int z) {
		BlockPos pos = world.immutablePos(x, y, z);
		return pos == null || world.world.canPlace(pos, state);
	}

	public static boolean hasWater(BlockState state) {
		return state.getFluidState().isIn(TagsVersions.water());
	}

	public static boolean hasLava(BlockState state) {
		return state.getFluidState().isIn(TagsVersions.lava());
	}

	public static boolean hasSoulLava(BlockState state) {
		return state.getFluidState().isIn(BigGlobeFluidTags.SOUL_LAVA);
	}

	public static boolean hasFluid(BlockState state) {
		return !state.getFluidState().isEmpty();
	}
}