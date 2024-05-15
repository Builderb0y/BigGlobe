package builderb0y.bigglobe.noise;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.noise.resample.*;
import builderb0y.bigglobe.noise.resample.derivatives.*;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid1D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid2D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid3D;
import builderb0y.bigglobe.settings.Seed;

import static org.junit.jupiter.api.Assertions.*;

public class ResampleGridTest {

	public static final long
		GRID_SEED = Permuter.stafford(12345L),
		WORLD_SEED = Permuter.stafford(54321L);
	public static final int
		GRID_SCALE_X = 16,
		GRID_SCALE_Y = 16,
		GRID_SCALE_Z = 16,
		ARRAY_SIZE = 64,
		START_X = 8,
		START_Y = 8,
		START_Z = 8;
	public static final double
		VALUE_EPSILON = 0.00000000000001D,
		DELTA_EPSILON = 0.02D;

	static {
		Grid.TESTING.setTrue();
	}

	@Test
	public void test1D() {
		NumberArray
			gridScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE),
			derivativeScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE);
		WhiteNoiseGrid1D source = new WhiteNoiseGrid1D(new Seed(GRID_SEED), 1.0D);
		test1D(
			new LinearResampleGrid1D(source, GRID_SCALE_X),
			new LinearDerivativeXResampleGrid1D(source, GRID_SCALE_X),
			gridScratch,
			derivativeScratch
		);
		test1D(
			new SmoothResampleGrid1D(source, GRID_SCALE_X),
			new SmoothDerivativeXResampleGrid1D(source, GRID_SCALE_X),
			gridScratch,
			derivativeScratch
		);
		test1D(
			new SmootherResampleGrid1D(source, GRID_SCALE_X),
			new SmootherDerivativeXResampleGrid1D(source, GRID_SCALE_X),
			gridScratch,
			derivativeScratch
		);
		test1D(
			new CubicResampleGrid1D(source, GRID_SCALE_X),
			new CubicDerivativeXResampleGrid1D(source, GRID_SCALE_X),
			gridScratch,
			derivativeScratch
		);
	}

	public void test1D(Grid1D grid, Grid1D derivative, NumberArray gridScratch, NumberArray derivativeScratch) {
		grid.getBulkX(WORLD_SEED, START_X, gridScratch);
		derivative.getBulkX(WORLD_SEED, START_X, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, index + START_X), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivative.getValue(WORLD_SEED, index + START_X), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid1D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid1D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
	}

	@Test
	public void test2D() {
		NumberArray
			gridScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE),
			derivativeScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE);
		WhiteNoiseGrid2D source = new WhiteNoiseGrid2D(new Seed(GRID_SEED), 1.0D);
		test2D(
			new LinearResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new LinearDerivativeXResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new LinearDerivativeYResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			gridScratch,
			derivativeScratch
		);
		test2D(
			new SmoothResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new SmoothDerivativeXResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new SmoothDerivativeYResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			gridScratch,
			derivativeScratch
		);
		test2D(
			new SmootherResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new SmootherDerivativeXResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new SmootherDerivativeYResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			gridScratch,
			derivativeScratch
		);
		test2D(
			new CubicResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new CubicDerivativeXResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			new CubicDerivativeYResampleGrid2D(source, GRID_SCALE_X, GRID_SCALE_Y),
			gridScratch,
			derivativeScratch
		);
	}

	public void test2D(Grid2D grid, Grid2D derivativeX, Grid2D derivativeY, NumberArray gridScratch, NumberArray derivativeScratch) {
		grid.getBulkX(WORLD_SEED, START_X, START_Y, gridScratch);
		derivativeX.getBulkX(WORLD_SEED, START_X, START_Y, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, index + START_X, START_Y), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivativeX.getValue(WORLD_SEED, index + START_X, START_Y), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid2D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid2D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
		derivativeX.getBulkY(WORLD_SEED, START_X, START_Y, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeX.getValue(WORLD_SEED, START_X, index + START_Y), VALUE_EPSILON);
		}

		grid.getBulkY(WORLD_SEED, START_X, START_Y, gridScratch);
		derivativeY.getBulkY(WORLD_SEED, START_X, START_Y, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, START_X, index + START_Y), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivativeY.getValue(WORLD_SEED, START_X, index + START_Y), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid2D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid2D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
		derivativeY.getBulkX(WORLD_SEED, START_X, START_Y, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeY.getValue(WORLD_SEED, index + START_X, START_Y), VALUE_EPSILON);
		}
	}

	@Test
	public void test3D() {
		NumberArray
			gridScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE),
			derivativeScratch = NumberArray.allocateDoublesHeap(ARRAY_SIZE);
		WhiteNoiseGrid3D source = new WhiteNoiseGrid3D(new Seed(GRID_SEED), 1.0D);
		test3D(
			new LinearResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new LinearDerivativeXResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new LinearDerivativeYResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new LinearDerivativeZResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			gridScratch,
			derivativeScratch
		);
		test3D(
			new SmoothResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmoothDerivativeXResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmoothDerivativeYResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmoothDerivativeZResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			gridScratch,
			derivativeScratch
		);
		test3D(
			new SmootherResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmootherDerivativeXResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmootherDerivativeYResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new SmootherDerivativeZResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			gridScratch,
			derivativeScratch
		);
		test3D(
			new CubicResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new CubicDerivativeXResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new CubicDerivativeYResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			new CubicDerivativeZResampleGrid3D(source, GRID_SCALE_X, GRID_SCALE_Y, GRID_SCALE_Z),
			gridScratch,
			derivativeScratch
		);
	}

	public void test3D(Grid3D grid, Grid3D derivativeX, Grid3D derivativeY, Grid3D derivativeZ, NumberArray gridScratch, NumberArray derivativeScratch) {
		grid.getBulkX(WORLD_SEED, START_X, START_Y, START_Z, gridScratch);
		derivativeX.getBulkX(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, index + START_X, START_Y, START_Z), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivativeX.getValue(WORLD_SEED, index + START_X, START_Y, START_Z), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid3D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid3D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
		derivativeX.getBulkY(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeX.getValue(WORLD_SEED, START_X, index + START_Y, START_Z), VALUE_EPSILON);
		}
		derivativeX.getBulkZ(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeX.getValue(WORLD_SEED, START_X, START_Y, index + START_Z), VALUE_EPSILON);
		}

		grid.getBulkY(WORLD_SEED, START_X, START_Y, START_Z, gridScratch);
		derivativeY.getBulkY(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, START_X, index + START_Y, START_Z), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivativeY.getValue(WORLD_SEED, START_X, index + START_Y, START_Z), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid3D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid3D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
		derivativeY.getBulkX(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeY.getValue(WORLD_SEED, index + START_X, START_Y, START_Z), VALUE_EPSILON);
		}
		derivativeY.getBulkZ(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeY.getValue(WORLD_SEED, START_X, START_Y, index + START_Z), VALUE_EPSILON);
		}

		grid.getBulkZ(WORLD_SEED, START_X, START_Y, START_Z, gridScratch);
		derivativeZ.getBulkZ(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(gridScratch.getD(index), grid.getValue(WORLD_SEED, START_X, START_Y, index + START_Z), VALUE_EPSILON);
			assertEquals(derivativeScratch.getD(index), derivativeZ.getValue(WORLD_SEED, START_X, START_Y, index + START_Z), VALUE_EPSILON);
			if (index > 0 && !(grid instanceof LinearResampleGrid3D && ((index + START_X) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) - derivativeScratch.getD(index), gridScratch.getD(index - 1), DELTA_EPSILON);
			}
			if (index < ARRAY_SIZE - 1 && !(grid instanceof LinearResampleGrid3D && ((index + (START_X + 1)) & (GRID_SCALE_X - 1)) == 0)) {
				assertEquals(gridScratch.getD(index) + derivativeScratch.getD(index), gridScratch.getD(index + 1), DELTA_EPSILON);
			}
		}
		derivativeZ.getBulkX(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeZ.getValue(WORLD_SEED, index + START_X, START_Y, START_Z), VALUE_EPSILON);
		}
		derivativeZ.getBulkY(WORLD_SEED, START_X, START_Y, START_Z, derivativeScratch);
		for (int index = 0; index < ARRAY_SIZE; index++) {
			assertEquals(derivativeScratch.getD(index), derivativeZ.getValue(WORLD_SEED, START_X, index + START_Y, START_Z), VALUE_EPSILON);
		}
	}
}