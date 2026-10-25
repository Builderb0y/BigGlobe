package builderb0y.bigglobe.mixins;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StairBlock.class)
public abstract class StairsBlock_MirrorProperly extends Block {

	@Shadow
	@Final
	public static EnumProperty<StairsShape> SHAPE;

	public StairsBlock_MirrorProperly(Properties settings) {
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
	public BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror != Mirror.NONE) {
			Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			StairsShape shape = state.getValue(SHAPE);
			state = state.setValue(
				SHAPE, switch (shape) {
					case STRAIGHT -> StairsShape.STRAIGHT;
					case INNER_LEFT -> StairsShape.INNER_RIGHT;
					case INNER_RIGHT -> StairsShape.INNER_LEFT;
					case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
					case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
				}
			);
			if (direction.getAxis() == (mirror == Mirror.LEFT_RIGHT ? Axis.Z : Axis.X)) {
				state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction.getOpposite());
			}
		}
		return state;
	}
}