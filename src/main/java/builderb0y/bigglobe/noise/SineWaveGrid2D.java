package builderb0y.bigglobe.noise;

import java.util.random.RandomGenerator;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.settings.Seed;

public class SineWaveGrid2D implements Grid2D {

	public final Seed seed;
	public final double scale;
	public final double amplitude;
	public final @VerifyIntRange(min = 0, max = 64) int iterations;
	public transient PhaseData phaseData;

	public SineWaveGrid2D(Seed seed, double scale, double amplitude, int iterations) {
		this.seed       = seed;
		this.scale      = scale;
		this.amplitude  = amplitude;
		this.iterations = iterations;
	}

	public PhaseData getPhaseData(long seed) {
		PhaseData data = this.phaseData;
		if (data == null || data.seed != seed) {
			double[] angles    = this.generateBlueNoise(seed ^ 0x7FDD3387380B99C5L);
			double[] cosAngles = new double[this.iterations];
			double[] sinAngles = new double[this.iterations];
			for (int index = this.iterations; --index >= 0;) {
				cosAngles[index] = Math.cos(angles[index]);
				sinAngles[index] = Math.sin(angles[index]);
			}
			double[] offsets = this.generateBlueNoise(seed ^ 0x221E125F88ABE72DL);
			data = this.phaseData = new PhaseData(seed, cosAngles, sinAngles, offsets);
		}
		return data;
	}

	public double[] generateBlueNoise(long salt) {
		RandomGenerator random = new Permuter(this.seed.xor(salt));
		double[] result = new double[this.iterations];
		for (int point = 0; point < this.iterations; point++) {
			double bestDistance = 0.0D;
			double bestValue = Double.NaN;
			for (int attempt = 0; attempt <= point; attempt++) {
				double newValue = random.nextDouble() * BigGlobeMath.TAU;
				double newDistance = Double.POSITIVE_INFINITY;
				for (int check = 0; check < point; check++) {
					double checkDistance = Math.abs(result[check] - newValue);
					newDistance = Math.min(newDistance, Math.min(checkDistance, BigGlobeMath.TAU - check));
				}
				if (newDistance > bestDistance) {
					bestValue = newValue;
					bestDistance = newDistance;
				}
			}
			result[point] = bestValue;
		}
		return result;
	}

	@Override
	public double maxValue() {
		return Math.abs(this.amplitude) * this.iterations;
	}

	@Override
	public double minValue() {
		return -Math.abs(this.amplitude) * this.iterations;
	}

	@Override
	public double getValue(long seed, int x, int y) {
		PhaseData data = this.getPhaseData(seed);
		double sum = 0.0;
		for (int iteration = 0; iteration < this.iterations; iteration++) {
			sum += Math.sin(x * data.cosAngles[iteration] + y * data.sinAngles[iteration] + data.offsets[iteration]);
		}
		return sum;
	}

	public static record PhaseData(long seed, double[] cosAngles, double[] sinAngles, double[] offsets) {}
}