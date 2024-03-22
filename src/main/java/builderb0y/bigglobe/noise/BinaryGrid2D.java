package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class BinaryGrid2D extends BinaryGrid implements Grid2D {

	public BinaryGrid2D(Seed salt, double amplitude, double chance) {
		super(salt, amplitude, chance);
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return Permuter.toChancedBoolean(Permuter.permute(this.salt.xor(seed), x, y), this.chance) ? this.amplitude : -this.amplitude;
	}
}