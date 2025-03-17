package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.noise.processing.SummingGrid2D;
import builderb0y.bigglobe.noise.resample.SmoothGrid2D;
import builderb0y.bigglobe.settings.Seed;

public class NoiseStatistics {

	public static void main(String[] args) {
		Grid.TESTING.setTrue();
		long seed = Permuter.stafford(12345L);
		Grid2D grid = new SummingGrid2D(
			new SmoothGrid2D(new Seed(Permuter.stafford(1L)), 0.5D,     64, 64),
			new SmoothGrid2D(new Seed(Permuter.stafford(2L)), 0.25D,    32, 32),
			new SmoothGrid2D(new Seed(Permuter.stafford(3L)), 0.125D,   16, 16),
			new SmoothGrid2D(new Seed(Permuter.stafford(4L)), 0.0625D,   8,  8),
			new SmoothGrid2D(new Seed(Permuter.stafford(5L)), 0.03125D,  4,  4)
		);
		NumberArray array = NumberArray.allocateDoublesHeap(16384);
		int[] buckets = new int[64];
		int max = 0;
		for (int z = 0; z < 16384; z++) {
			grid.getBulkX(seed, 0, z, array);
			for (int x = 0; x < 16384; x++) {
				max = Math.max(max, ++buckets[(int)(curve(array.getD(x)) * 32.0D + 32.0D)]);
			}
		}
		for (int bucket : buckets) {
			System.out.println("#".repeat(bucket * 128 / max));
		}
	}

	public static double curve(double value) {
		//double x2 = value * value;

		//final double d = 2048.0D;
		//double poly = (((((((-429/d * x2 + 3465/d) * x2 - 12285/d) * x2 + 25025/d) * x2 - 32175/d) * x2 + 27027/d) * x2 - 15015/d) * x2 + 6435/d) * value;

		//final double d = 32768.0D;
		//double poly = ((((((((6435.0D / d * x2 - 58344.0D / d) * x2 + 235620.0D / d) * x2 - 556920.0D / d) * x2 + 850850.0D / d) * x2 - 875160.0D / d) * x2 + 612612.0D / d) * x2 - 291720.0D / d) * x2 + 109395.0D / d) * value;

		double poly = smooth(smooth(smooth(value)));
		return (poly * poly * 0.125D + 0.875D) * poly;
	}

	public static double smooth(double x) {
		return (-0.5D * x * x + 1.5D) * x;
	}

	public static double smooth2(double x) {
		double x2 = x * x;
		final double d = 8;
		return ((3/d * x2 - 10/d) * x2 + 15/d) * x;
	}

	public static double square(double x) {
		return x * x;
	}
}