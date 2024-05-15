package builderb0y.bigglobe.noise.source;

import builderb0y.bigglobe.noise.Grid3D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.noise.processing.AbstractGrid;
import builderb0y.bigglobe.settings.Seed;

public class WhiteNoiseGrid3D extends AbstractGrid implements Grid3D {

	public WhiteNoiseGrid3D(Seed salt, double amplitude) {
		super(salt, amplitude);
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return Permuter.toUniformDouble(Permuter.permute(this.salt.xor(seed), x, y, z)) * this.amplitude;
	}
}