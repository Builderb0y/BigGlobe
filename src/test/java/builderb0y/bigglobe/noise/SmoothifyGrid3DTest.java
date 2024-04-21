package builderb0y.bigglobe.noise;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.settings.Seed;

import static org.junit.jupiter.api.Assertions.*;

public class SmoothifyGrid3DTest {
	public static final long  gridSalt  = 0x60AA0310EEA89B50L;
	public static final long worldSeed  = 0x9A83AD562CD179C2L;
	public static final int sampleCount = 32;

	@Test
	public void testAll() {
		Grid.TESTING.setTrue();
		Grid3D grid = new SmoothGrid3D(new Seed(gridSalt), 1.0F, 16, 8, 16);
		NumberArray values = NumberArray.allocateDoublesHeap(sampleCount);
		for (int x = -32; x < 32; x++) {
			for (int z = -32; z < 32; z++) {
				for (int y = -32; y < 32; y++) {
					this.testPosition(grid, values, x, y, z);
				}
			}
		}
	}

	public void testPosition(Grid3D grid, NumberArray values, int x, int y, int z) {
		grid.getBulkX(worldSeed, x, y, z, values);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x + index, y, z), values.getD(index), 0.000001D);
		}
		this.checkContinuous(values);
		grid.getBulkY(worldSeed, x, y, z, values);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x, y + index, z), values.getD(index), 0.000001D);
		}
		this.checkContinuous(values);
		grid.getBulkZ(worldSeed, x, y, z, values);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x, y, z + index), values.getD(index), 0.000001D);
		}
		this.checkContinuous(values);
	}

	public void checkContinuous(NumberArray values) {
		for (int index = 1; index < sampleCount - 1; index++) {
			assertEquals(values.getD(index), (values.getD(index - 1) + values.getD(index + 1)) * 0.5F, 0.125F);
		}
	}

	@Test
	public void testDirect() {

	}

	public String dumpValues(float[] values) {
		StringBuilder builder = new StringBuilder(sampleCount << 4);
		for (int index = 0; index < sampleCount; index++) {
			builder.append(index).append(',').append(values[index]).append('\n');
		}
		return builder.toString();
	}
}