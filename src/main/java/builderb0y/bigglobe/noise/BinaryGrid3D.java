package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.settings.Seed;

public class BinaryGrid3D extends BinaryGrid implements Grid3D {

	public BinaryGrid3D(Seed salt, double amplitude, double chance) {
		super(salt, amplitude, chance);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return Permuter.toChancedBoolean(Permuter.permute(this.salt.xor(seed), x, y, z), this.chance) ? this.amplitude : -this.amplitude;
	}
}