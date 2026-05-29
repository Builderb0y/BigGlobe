package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.*;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;

import static net.minecraft.world.level.block.state.properties.RailShape.*;

@Mixin(RailBlock.class)
public class RailBlock_RotateProperly extends Block {

	@Shadow
	public static @Final EnumProperty<RailShape> SHAPE;
	@Unique
	private static final RailShape[]
		BIGGLOBE_STRAIGHT = { NORTH_SOUTH, EAST_WEST },
		BIGGLOBE_ASCENDING = { ASCENDING_NORTH, ASCENDING_EAST, ASCENDING_SOUTH, ASCENDING_WEST },
		BIGGLOBE_CURVED = { SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST };
	@Unique
	private static final int[]
		BIGGLOBE_ORDINAL_MAP = { 0, 1, 1, 3, 0, 2, 0, 1, 2, 3 };
	@Unique
	private static final RailShape[][]
		BIGGLOBE_SHAPES = { BIGGLOBE_STRAIGHT, BIGGLOBE_STRAIGHT, BIGGLOBE_ASCENDING, BIGGLOBE_ASCENDING, BIGGLOBE_ASCENDING, BIGGLOBE_ASCENDING, BIGGLOBE_CURVED, BIGGLOBE_CURVED, BIGGLOBE_CURVED, BIGGLOBE_CURVED };

	public RailBlock_RotateProperly(Properties settings) {
		super(settings);
	}

	/**
	@author Builderb0y
	@reason the vanilla logic is missing a break statement
	in its massive mess of nested switch statements,
	causing some rotations to be performed incorrectly.
	this breaks my surface mineshafts.

	this is the 2nd time I've improved some rotate/mirror
	logic in vanilla code and somehow the fixed version
	is shorter than the original in both cases.
	*/
	@Override
	@Overwrite
	public BlockState rotate(BlockState state, Rotation rotation) {
		RailShape oldShape = state.getValue(SHAPE);
		RailShape[] shapes = BIGGLOBE_SHAPES[oldShape.ordinal()];
		int oldOrdinal = BIGGLOBE_ORDINAL_MAP[oldShape.ordinal()];
		int newOrdinal = (oldOrdinal + rotation.ordinal()) & (shapes.length - 1);
		RailShape newShape = shapes[newOrdinal];
		return state.setValue(SHAPE, newShape);
	}
}