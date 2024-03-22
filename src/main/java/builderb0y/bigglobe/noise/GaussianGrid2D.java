package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class GaussianGrid2D extends GaussianGrid implements Grid2D {

	public GaussianGrid2D(Seed salt, double amplitude, int iterations) {
		super(salt, amplitude, iterations);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		seed = this.salt.xor(seed);
		double sum = 0.0D;
		for (int iteration = this.iterations; --iteration >= 0;) {
			sum += Permuter.toUniformDouble(Permuter.permute(seed, x, y, iteration));
		}
		return sum * this.amplitude / this.iterations;
	}
}