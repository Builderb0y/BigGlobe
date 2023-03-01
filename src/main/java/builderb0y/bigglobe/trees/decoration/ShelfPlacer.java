package builderb0y.bigglobe.trees.decoration;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import builderb0y.bigglobe.trees.TreeGenerator;

public abstract class ShelfPlacer {

	public static final Direction[] HORIZONTAL_DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
	public static final Direction[] REVERSE_DIRECTIONS = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST };
	public static final BooleanProperty[] HORIZONTAL_PROPERTIES = { Properties.NORTH, Properties.SOUTH, Properties.EAST, Properties.WEST };
	public static final BooleanProperty[] REVERSE_PROPERTIES = { Properties.SOUTH, Properties.NORTH, Properties.WEST, Properties.EAST };

	public final BlockState state;

	public ShelfPlacer(BlockState state) {
		this.state = state;
	}

	public static ShelfPlacer create(BlockState state) {
		if (state.contains(Properties.UP)) state = state.with(Properties.UP, Boolean.TRUE);
		if (state.contains(Properties.DOWN)) state = state.with(Properties.DOWN, Boolean.FALSE);
		int count = 0;
		for (int i = 0; i < 4; i++) {
			if (state.contains(HORIZONTAL_PROPERTIES[i])) {
				state = state.with(HORIZONTAL_PROPERTIES[i], Boolean.TRUE);
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
	public abstract void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.Mutable offsetPos);

	public static class NoSides extends ShelfPlacer {

		public NoSides(BlockState state) {
			super(state);
		}

		@Override
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.Mutable offsetPos) {
			generator.worldQueue.setBlockState(pos, this.state);
		}
	}

	public static class AllSides extends ShelfPlacer {

		public AllSides(BlockState state) {
			super(state);
		}

		@Override
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.Mutable offsetPos) {
			BlockState ourState = this.state;
			for (int i = 0; i < 4; i++) {
				BlockState otherState = generator.worldQueue.getBlockState(
					offsetPos.set(
						pos.getX() + HORIZONTAL_DIRECTIONS[i].getOffsetX(),
						pos.getY(),
						pos.getZ() + HORIZONTAL_DIRECTIONS[i].getOffsetZ()
					)
				);
				if (ourState.getBlock() == otherState.getBlock()) {
					ourState = ourState.with(HORIZONTAL_PROPERTIES[i], Boolean.FALSE);
					otherState = otherState.with(REVERSE_PROPERTIES[i], Boolean.FALSE);
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
		public void queueBlockAt(TreeGenerator generator, BlockPos pos, BlockPos.Mutable offsetPos) {
			BlockState ourState = this.state;
			for (int i = 0; i < 4; i++) {
				BlockState otherState = generator.worldQueue.getBlockState(
					offsetPos.set(
						pos.getX() + HORIZONTAL_DIRECTIONS[i].getOffsetX(),
						pos.getY(),
						pos.getZ() + HORIZONTAL_DIRECTIONS[i].getOffsetZ()
					)
				);
				if (ourState.getBlock() == otherState.getBlock()) {
					if (ourState.contains(HORIZONTAL_PROPERTIES[i])) {
						ourState = ourState.with(HORIZONTAL_PROPERTIES[i], Boolean.FALSE);
					}
					if (otherState.contains(REVERSE_PROPERTIES[i])) {
						otherState = otherState.with(REVERSE_PROPERTIES[i], Boolean.FALSE);
						generator.worldQueue.setBlockState(offsetPos, otherState);
					}
				}
			}
			generator.worldQueue.setBlockState(pos, ourState);
		}
	}
}