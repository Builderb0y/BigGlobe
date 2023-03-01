package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class WhiteNoiseGrid1D extends AbstractGrid implements Grid1D {

	public WhiteNoiseGrid1D(Seed salt, double amplitude) {
		super(salt, amplitude);
	}

	@Override
	public double getValue(long seed, int x) {
		return Permuter.toUniformDouble(Permuter.permute(this.salt.xor(seed), x)) * this.amplitude;
	}
}