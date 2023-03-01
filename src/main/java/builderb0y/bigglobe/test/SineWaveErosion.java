package builderb0y.bigglobe.test;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class SineWaveErosion {

	public final int x, z;
	public double height, derivativeX, derivativeZ;

	public SineWaveErosion(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public SineWaveErosion computeHeight(long seed) {
		double x = this.x / 512.0D;
		double z = this.z / 512.0D;
		double height = 0.0D, derivativeX = 0.0D, derivativeZ = 0.0D;
		long angleSeed = seed ^ 0xEEF0B2268BBC584DL;
		long phaseSeed = seed ^ 0xA600CD5D0EBC8EE9L;
		for (int iteration = 0; iteration < 896; iteration++) {
			double angle = Permuter.nextPositiveDouble(angleSeed += Permuter.PHI64) * BigGlobeMath.TAU;
			double phase = Permuter.nextPositiveDouble(phaseSeed += Permuter.PHI64) * BigGlobeMath.TAU;
			double baseAmplitude = Math.exp(iteration / -128.0D) / BigGlobeMath.LN_2;

			double unitX = Math.cos(angle);
			double unitZ = Math.sin(angle);
			double project = x * unitX + z * unitZ + phase;
			double derivativeScale1 = 1.0D / (Math.sqrt(BigGlobeMath.squareD(derivativeX, derivativeZ)) + (1.0D / 256.0D));
			double adjustedAmplitude = 1.0D - BigGlobeMath.squareD(
				unitX * derivativeX * derivativeScale1 +
				unitZ * derivativeZ * derivativeScale1
			);

			height += Math.sin(project / baseAmplitude) * baseAmplitude * adjustedAmplitude;
			double derivativeScale2 = (1.0D / 512.0D) * Math.cos(project / baseAmplitude) * adjustedAmplitude;
			derivativeX += unitX * derivativeScale2;
			derivativeZ += unitZ * derivativeScale2;
		}
		this.height      = 32.0D * height;
		this.derivativeX = 32.0D * derivativeX;
		this.derivativeZ = 32.0D * derivativeZ;

		return this;
	}
}