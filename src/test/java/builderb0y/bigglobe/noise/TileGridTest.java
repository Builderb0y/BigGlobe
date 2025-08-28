package builderb0y.bigglobe.noise;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.noise.source.WhiteNoiseGrid1D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid2D;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid3D;
import builderb0y.bigglobe.settings.Seed;

import static org.junit.jupiter.api.Assertions.*;

public class TileGridTest {

	public static final long
		GRID_SEED  = Permuter.stafford(12345L),
		WORLD_SEED = Permuter.stafford(54321L);
	public static final double
		VALUE_EPSILON = 0.00000000000001D;

	static {
		Grid.TESTING.setTrue();
	}

	@Test
	public void test1D() {
		TileGrid1D grid = new TileGrid1D(new WhiteNoiseGrid1D(new Seed(GRID_SEED), 1.0D), 8);
		for (int length = 8; length <= 24; length++) {
			NumberArray array = NumberArray.allocateDoublesHeap(length);
			for (int startX = 0; startX <= 16; startX++) {
				grid.getBulkX(WORLD_SEED, startX, array);
				for (int index = 0; index < length; index++) {
					assertEquals(grid.getValue(WORLD_SEED, startX + index), array.getD(index), VALUE_EPSILON);
				}
			}
		}
	}

	@Test
	public void test2D() {
		TileGrid2D grid = new TileGrid2D(new WhiteNoiseGrid2D(new Seed(GRID_SEED), 1.0D), 8, 8);
		for (int length = 8; length <= 24; length++) {
			NumberArray array = NumberArray.allocateDoublesHeap(length);
			for (int startX = 0; startX <= 16; startX++) {
				for (int startY = 0; startY <= 16; startY++) {
					grid.getBulkX(WORLD_SEED, startX, startY, array);
					for (int index = 0; index < length; index++) {
						assertEquals(grid.getValue(WORLD_SEED, startX + index, startY), array.getD(index), VALUE_EPSILON);
					}
					grid.getBulkY(WORLD_SEED, startX, startY, array);
					for (int index = 0; index < length; index++) {
						assertEquals(grid.getValue(WORLD_SEED, startX, startY + index), array.getD(index), VALUE_EPSILON);
					}
				}
			}
		}
	}

	@Test
	public void test3D() {
		TileGrid3D grid = new TileGrid3D(new WhiteNoiseGrid3D(new Seed(GRID_SEED), 1.0D), 8, 8, 8);
		for (int length = 8; length <= 24; length++) {
			NumberArray array = NumberArray.allocateDoublesHeap(length);
			for (int startX = 0; startX <= 16; startX++) {
				for (int startY = 0; startY <= 16; startY++) {
					for (int startZ = 0; startZ <= 16; startZ++) {
						grid.getBulkX(WORLD_SEED, startX, startY, startZ, array);
						for (int index = 0; index < length; index++) {
							assertEquals(grid.getValue(WORLD_SEED, startX + index, startY, startZ), array.getD(index), VALUE_EPSILON);
						}
						grid.getBulkY(WORLD_SEED, startX, startY, startZ, array);
						for (int index = 0; index < length; index++) {
							assertEquals(grid.getValue(WORLD_SEED, startX, startY + index, startZ), array.getD(index), VALUE_EPSILON);
						}
						grid.getBulkZ(WORLD_SEED, startX, startY, startZ, array);
						for (int index = 0; index < length; index++) {
							assertEquals(grid.getValue(WORLD_SEED, startX, startY, startZ + index), array.getD(index), VALUE_EPSILON);
						}
					}
				}
			}
		}
	}
}