package builderb0y.bigglobe.noise;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.settings.Seed.NumberSeed;

import static org.junit.jupiter.api.Assertions.*;

public class SmoothifyGrid3DTest {
	public static final long  gridSalt  = 0x60AA0310EEA89B50L;
	public static final long worldSeed  = 0x9A83AD562CD179C2L;
	public static final int sampleCount = 32;

	@Test
	public void testAll() {
		ValueGrid3D grid = new SmoothGrid3D(new NumberSeed(gridSalt), 1.0F, 16, 8, 16);
		double[] values = new double[sampleCount];
		for (int x = -64; x < 64; x++) {
			for (int z = -64; z < 64; z++) {
				for (int y = -64; y < 64; y++) {
					this.testPosition(grid, values, x, y, z);
				}
			}
		}
	}

	public void testPosition(ValueGrid3D grid, double[] values, int x, int y, int z) {
		grid.getBulkX(worldSeed, x, y, z, values, sampleCount);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x + index, y, z), values[index], 0.000001F);
		}
		this.checkContinuous(values);
		grid.getBulkY(worldSeed, x, y, z, values, sampleCount);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x, y + index, z), values[index], 0.000001F);
		}
		this.checkContinuous(values);
		grid.getBulkZ(worldSeed, x, y, z, values, sampleCount);
		for (int index = 0; index < sampleCount; index++) {
			assertEquals(grid.getValue(worldSeed, x, y, z + index), values[index], 0.000001F);
		}
		this.checkContinuous(values);
	}

	public void checkContinuous(double[] values) {
		for (int index = 1; index < sampleCount - 1; index++) {
			assertEquals(values[index], (values[index - 1] + values[index + 1]) * 0.5F, 0.125F);
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