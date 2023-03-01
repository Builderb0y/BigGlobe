package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class WhiteNoiseGrid2D extends AbstractGrid implements Grid2D {

	public WhiteNoiseGrid2D(Seed salt, double amplitude) {
		super(salt, amplitude);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return Permuter.toUniformDouble(Permuter.permute(this.salt.xor(seed), x, y)) * this.amplitude;
	}
}