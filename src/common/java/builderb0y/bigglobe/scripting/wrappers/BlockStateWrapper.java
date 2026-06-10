package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.codecs.BlockStateCoder.BlockProperties;
import builderb0y.bigglobe.fluids.BigGlobeFluidTags;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.scripting.wrappers.tags.BlockTag;
import builderb0y.bigglobe.scripting.wrappers.tags.TagParser;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.ConstantFactory;
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
		WITH = MethodInfo.getMethod(BlockStateWrapper.class, "with"),
		WITH_NULLABLE = MethodInfo.getMethod(BlockStateWrapper.class, "withNullable");
	public static final TagParser
		TAG_PARSER = new TagParser("BlockTag", BlockTag.class, "BlockState", MethodInfo.inCaller("isIn"));

	public static BlockState getState(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		if (id == null) return null;
		BlockProperties block;
		try {
			block = (
				BlockStateCoder
					.decodeStateWithMissingErrors(BlockStateCoder.findBlockRegistry(), id)
					.unwrapEager(BigGlobeMod.LOGGER::warn, RuntimeException::new)
			);
			if (!block.enabled()) {
				throw new RuntimeException("Disabled block: " + id);
			}
		}
		catch (Exception exception) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw exception;
		}
		Set<Property<?>> missing = block.missing();
		if (!missing.isEmpty()) {
			ScriptLogger.LOGGER.warn("Missing properties for state " + id + ": " + missing);
		}
		return block.state();
	}

	public static BlockState getState(String id, int flags) {
		if (id == null) return null;
		//this is the non-constant code path, so we will skip logging of missing properties here.
		try {
			BlockProperties block = (
				BlockStateCoder
					.decodeState(BlockStateCoder.findBlockRegistry(), id)
					.unwrapEager(BigGlobeMod.LOGGER::warn, RuntimeException::new)
			);
			if (!block.enabled()) {
				throw new RuntimeException("Disabled block: " + id);
			}
			return block.state();
		}
		catch (Exception exception) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	public static BlockState getDefaultState(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return getDefaultState(id, flags);
	}

	public static BlockState getDefaultState(String id, int flags) {
		if (id == null) return null;
		try {
			Identifier identifier = IdentifierVersions.create(id);
			if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
				return BuiltInRegistries.BLOCK.getValue(identifier).defaultBlockState();
			}
			else {
				throw new RuntimeException("Unknown block: " + id);
			}
		}
		catch (RuntimeException exception) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	public static boolean isIn(BlockState state, BlockTag tag) {
		return tag.list.contains(state.typeHolder());
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
		return state.canOcclude();
	}

	public static boolean hasCollision(BlockState state) {
		return !state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
	}

	public static boolean hasFullCubeCollision(BlockState state) {
		return Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
	}

	public static boolean hasFullCubeOutline(BlockState state) {
		return Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
	}

	public static BlockState rotate(BlockState state, int rotation) {
		return state.rotate(Directions.scriptRotation(rotation));
	}

	public static BlockState mirror(BlockState state, String axis) {
		return state.mirror(Directions.scriptMirror(axis));
	}

	@SuppressWarnings("unchecked")
	public static <C extends Comparable<C>> @Nullable C getProperty(BlockState state, String name) {
		Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
		if (property == null) return null;
		Comparable<?> value = state.getValue(property);
		if (value instanceof StringRepresentable e) {
			value = e.getSerializedName();
		}
		return (C)(value);
	}

	public static BlockState withNullable(BlockState state, String name, Comparable<?> value) {
		return state == null ? null : with(state, name, value);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static BlockState with(BlockState state, String name, Comparable<?> value) {
		Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
		if (property == null) return state;
		if (value instanceof String string) {
			value = property.getValue(string).orElse(null);
			if (value == null) return state;
		}
		if (!property.getValueClass().isInstance(value)) return state;
		return state.setValue((Property)(property), (Comparable)(value));
	}

	public static boolean canPlaceAt(BlockState state, WorldWrapper world, int x, int y, int z) {
		BlockPos pos = world.immutablePos(x, y, z);
		return pos != null && BlockStateVersions.isReplaceable(world.world.getBlockState(pos)) && world.world.canPlace(pos, state);
	}

	public static boolean canStayAt(BlockState state, WorldWrapper world, int x, int y, int z) {
		BlockPos pos = world.immutablePos(x, y, z);
		return pos == null || world.world.canPlace(pos, state);
	}

	public static boolean hasWater(BlockState state) {
		return state.getFluidState().is(FluidTags.WATER);
	}

	public static boolean hasLava(BlockState state) {
		return state.getFluidState().is(FluidTags.LAVA);
	}

	public static boolean hasSoulLava(BlockState state) {
		return state.getFluidState().is(BigGlobeFluidTags.SOUL_LAVA);
	}

	public static boolean hasFluid(BlockState state) {
		return !state.getFluidState().isEmpty();
	}
}