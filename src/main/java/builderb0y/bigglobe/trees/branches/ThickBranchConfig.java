package builderb0y.bigglobe.trees.branches;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.trees.TreeGenerator;

import static builderb0y.bigglobe.math.BigGlobeMath.squareD;

public class ThickBranchConfig extends BranchConfig {

	public static final double MIN_RADIUS = 0.8660254037844386D; //sqrt(0.5 ^ 2 + 0.5 ^ 2 + 0.5 ^ 2)

	public double currentRadius;

	public ThickBranchConfig(double angle, double length, long seed) {
		super(angle, length, seed);
	}

	@Override
	public void setBothLengths(TreeGenerator generator, double length, double fracLength) {
		super.setBothLengths(generator, length, fracLength);
		//mixLinear(generator.trunkConfig.currentRadius, MIN_SPHERE_RADIUS, 1.0D - squareD(1.0D - fracLength))
		this.currentRadius = Interpolator.mixLinear(MIN_RADIUS, generator.trunk.currentRadius, squareD(1.0D - fracLength));
	}

	public void project(TreeGenerator generator, int x, int z) {
		this.setLength(generator, (x - generator.trunk.currentX) * this.nx + (z - generator.trunk.currentZ) * this.nz);
	}

	public boolean isInRadius(int x, int y, int z) {
		return squareD(x - this.currentX, y - this.currentY, z - this.currentZ) < squareD(this.currentRadius);
	}
}