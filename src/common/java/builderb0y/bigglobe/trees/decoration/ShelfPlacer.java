package builderb0y.bigglobe.trees.decoration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import builderb0y.bigglobe.trees.TreeGenerator;

public abstract class ShelfPlacer {

	public static final Direction[] HORIZONTAL_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
	public static final Direction[] REVERSE_DIRECTIONS = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };
	public static final BooleanProperty[] HORIZONTAL_PROPERTIES = { BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST, BlockStateProperties.WEST };
	public static final BooleanProperty[] REVERSE_PROPERTIES = { BlockStateProperties.SOUTH, BlockStateProperties.NORTH, BlockStateProperties.WEST, BlockStateProperties.EAST };

	public final BlockState state;

	public ShelfPlacer(BlockState state) {
		this.state = state;
	}

	public static ShelfPlacer create(BlockState state) {
		if (state.hasProperty(BlockStateProperties.UP)) state = state.setValue(BlockStateProperties.UP, Boolean.TRUE);
		if (state.hasProperty(BlockStateProperties.DOWN)) state = state.setValue(BlockStateProperties.DOWN, Boolean.FALSE);
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (state.hasProperty(HORIZONTAL_PROPERTIES[i])) {
				state = state.setValue(HORIZONTAL_PROPERTIES[i], Boolean.TRUE);
				count++;
			}
		}
		if (count == 0) return new NoSides(state);
		if (count == 4) return new AllSides(state);
		return new SomeSides(state);
	}

	//callers need not set offsetPos to anything meaningful.
	//it's only a parameter to avoid unnecessary re-allocation.
	//placeAt() can set its position to whatever it wants.
	//callers also should not rely on placeAt() to do anything with offsetPos either.
	public abstract void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.MutableBlockPos offsetPos);

	public static class NoSides extends ShelfPlacer {

		public NoSides(BlockState state) {
			super(state);
		}

		@Override
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.MutableBlockPos offsetPos) {
			generator.worldQueue.setBlockState(pos, this.state);
		}
	}

	public static class AllSides extends ShelfPlacer {

		public AllSides(BlockState state) {
			super(state);
		}

		@Override
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.MutableBlockPos offsetPos) {
			BlockState ourState = this.state;
			for (int i = 0; i < 4; i++) {
				BlockState otherState = generator.worldQueue.getBlockState(
					offsetPos.set(
						pos.getX() + HORIZONTAL_DIRECTIONS[i].getStepX(),
						pos.getY(),
						pos.getZ() + HORIZONTAL_DIRECTIONS[i].getStepZ()
					)
				);
				if (ourState.getBlock() == otherState.getBlock()) {
					ourState = ourState.setValue(HORIZONTAL_PROPERTIES[i], Boolean.FALSE);
					otherState = otherState.setValue(REVERSE_PROPERTIES[i], Boolean.FALSE);
					generator.worldQueue.setBlockState(offsetPos, otherState);
				}
			}
			generator.worldQueue.setBlockState(pos, ourState);
		}
	}

	public static class SomeSides extends ShelfPlacer {

		public SomeSides(BlockState state) {
			super(state);
		}

		@Override
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.MutableBlockPos offsetPos) {
			BlockState ourState = this.state;
			for (int i = 0; i < 4; i++) {
				BlockState otherState = generator.worldQueue.getBlockState(
					offsetPos.set(
						pos.getX() + HORIZONTAL_DIRECTIONS[i].getStepX(),
						pos.getY(),
						pos.getZ() + HORIZONTAL_DIRECTIONS[i].getStepZ()
					)
				);
				if (ourState.getBlock() == otherState.getBlock()) {
					if (ourState.hasProperty(HORIZONTAL_PROPERTIES[i])) {
						ourState = ourState.setValue(HORIZONTAL_PROPERTIES[i], Boolean.FALSE);
					}
					if (otherState.hasProperty(REVERSE_PROPERTIES[i])) {
						otherState = otherState.setValue(REVERSE_PROPERTIES[i], Boolean.FALSE);
						generator.worldQueue.setBlockState(offsetPos, otherState);
					}
				}
			}
			generator.worldQueue.setBlockState(pos, ourState);
		}
	}
}