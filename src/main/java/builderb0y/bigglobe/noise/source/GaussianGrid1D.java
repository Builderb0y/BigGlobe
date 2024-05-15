package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public class GaussianGrid1D extends GaussianGrid implements Grid1D {

	public GaussianGrid1D(Seed salt, double amplitude, int iterations) {
		super(salt, amplitude, iterations);
	}

	@Override
	public double getValue(long seed, int x) {
		seed = this.salt.xor(seed);
		double sum = 0.0D;
		for (int iteration = this.iterations; --iteration >= 0;) {
			sum += Permuter.toUniformDouble(Permuter.permute(seed, x, iteration));
		}
		return sum * this.amplitude / this.iterations;
	}
}