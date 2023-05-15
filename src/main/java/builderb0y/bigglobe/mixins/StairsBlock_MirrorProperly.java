package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.*;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.StairShape;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

@Mixin(StairsBlock.class)
public abstract class StairsBlock_MirrorProperly extends Block {

	@Shadow @Final public static DirectionProperty FACING;
	@Shadow @Final public static EnumProperty<StairShape> SHAPE;

	public StairsBlock_MirrorProperly(Settings settings) {
		super(settings);
	}

	/**
	vanila bug fix: stairs do not mirror correctly.
	this breaks some of my structure placement logic.
	ironically, the fixed code is actually shorter than the broken code.
	@author Builderb0y
	@reason vanilla logic doesn't work correctly.
	*/
	@Override
	@Overwrite
	@Deprecated
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		Direction direction = state.get(FACING);
		StairShape shape = state.get(SHAPE);
		if (mirror != BlockMirror.NONE) {
			state = state.with(SHAPE, switch (shape) {
				case STRAIGHT    -> StairShape.STRAIGHT;
				case INNER_LEFT  -> StairShape.INNER_RIGHT;
				case INNER_RIGHT -> StairShape.INNER_LEFT;
				case OUTER_LEFT  -> StairShape.OUTER_RIGHT;
				case OUTER_RIGHT -> StairShape.OUTER_LEFT;
			});
			if (direction.getAxis() == (mirror == BlockMirror.LEFT_RIGHT ? Axis.Z : Axis.X)) {
				state = state.with(FACING, direction.getOpposite());
			}
		}
		return state;
	}
}