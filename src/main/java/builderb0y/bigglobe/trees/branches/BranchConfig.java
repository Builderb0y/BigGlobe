package builderb0y.bigglobe.trees.branches;

import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.trees.TreeGenerator;

public class BranchConfig {

	public final double angle, length;
	public final double nx, nz;
	public final Permuter permuter;
	public final long seed;

	public double currentLength, currentFracLength;
	public double currentX, currentY, currentZ;

	public BranchConfig(double angle, double length, long seed) {
		this.angle = angle;
		this.length = length;
		this.nx = Math.cos(angle);
		this.nz = Math.sin(angle);
		this.permuter = new Permuter(0L);
		this.seed = seed;
	}

	public void setBothLengths(TreeGenerator generator, double length, double fracLength) {
		this.currentLength = length;
		this.currentFracLength = fracLength;
		this.permuter.setSeed(this.seed);
		this.currentX = generator.trunk.currentX + this.nx * length;
		this.currentY = generator.trunk.currentY + generator.branches.heightGetter.evaluate(fracLength, generator.centerColumn, generator.trunk.currentY, this.permuter) * length;
		this.currentZ = generator.trunk.currentZ + this.nz * length;
	}

	public void setFracLength(TreeGenerator generator, double fracLength) {
		fracLength = MathHelper.clamp(fracLength, 0.0D, 1.0D);
		this.setBothLengths(generator, fracLength * this.length, fracLength);
	}

	public void setLength(TreeGenerator generator, double length) {
		length = MathHelper.clamp(length, 0.0D, this.length);
		this.setBothLengths(generator, length, length / this.length);
	}
}