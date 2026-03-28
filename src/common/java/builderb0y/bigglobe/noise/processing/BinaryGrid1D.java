package builderb0y.bigglobe.noise.processing;

import builderb0y.bigglobe.noise.Grid1D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;

public class BinaryGrid1D extends BinaryGrid implements Grid1D {

	public BinaryGrid1D(Seed salt, double amplitude, double chance) {
		super(salt, amplitude, chance);
	}

	@Override
	public double getValue(long seed, int x) {
		return Permuter.toChancedBoolean(Permuter.permute(this.salt.xor(seed), x), this.chance) ? this.amplitude : -this.amplitude;
	}
}