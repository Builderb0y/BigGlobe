package builderb0y.bigglobe.noise;

import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

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
		LongAdder[] buckets = new LongAdder[64];
		for (int index = 0; index < buckets.length; index++) {
			buckets[index] = new LongAdder();
		}
		int threads = Runtime.getRuntime().availableProcessors();
		int tasksPerThread = (16384 + threads - 1) / threads;
		IntStream.range(0, threads).parallel().forEach((int thread) -> {
			NumberArray array = NumberArray.allocateDoublesHeap(16384);
			int start = thread * tasksPerThread;
			int end = Math.min(thread * tasksPerThread + tasksPerThread, 16384);
			for (int z = start; z < end; z++) {
				grid.getBulkX(seed, 0, z, array);
				for (int x = 0; x < 16384; x++) {
					buckets[(int)(curve(array.getD(x)) * 64.0D)].increment();
				}
			}
		});
		long max = 0L;
		long[] sums = new long[buckets.length];
		for (int index = 0; index < buckets.length; index++) {
			max = Math.max(max, sums[index] = buckets[index].longValue());
		}
		for (long sum : sums) {
			System.out.println("#".repeat((int)(sum * 128L / max)));
		}
	}

	public static double curve(double value) {
		//double poly = smooth(smooth(smooth(value)));
		//return poly * 0.5D + 0.5D;
		//return (poly * poly * 0.0625D + 0.4375D) * poly + 0.5D;

		return square(square(1.0D - Math.abs(value)));
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