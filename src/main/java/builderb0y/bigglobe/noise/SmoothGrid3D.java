package builderb0y.bigglobe.noise;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.settings.Seed;

public class SmoothGrid3D extends ValueGrid3D {

	public SmoothGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(salt, amplitude, scaleX, scaleY, scaleZ);
	}

	/*
	//below is my (failed) attempt at vectorization.
	//the goal being to write some code that the JVM can vectorize easily.
	//I was planning on expanding it to looping in other directions too,
	//and then porting to CubicGrid3D... if it worked.
	//unfortunately, it did not result in any significant
	//performance gains, so it is commented out.

	public static final int ALIGNMENT = 4;

	public void vectorizedLoopY4(int startPosition, Int2DoubleFunction cornerGetter, NumberArray samples) {
		int sampleCount = samples.length();
		int scale = this.scaleY;
		double rcp = this.rcpY;
		double amplitude = this.amplitude;
		int cornerPos = Math.floorDiv(startPosition, scale);
		int frac = BigGlobeMath.modulus_BP(startPosition, scale);
		double value0 = cornerGetter.get(  cornerPos) * amplitude;
		double value1 = cornerGetter.get(++cornerPos) * amplitude;
		double diff = value1 - value0;
		int index = 0;
		//align.
		while ((frac & (ALIGNMENT - 1)) != 0) {
			double linearFrac = frac * rcp;
			double smoothFrac = linearFrac * linearFrac * (linearFrac * -2.0D + 3.0D);
			double lerp = smoothFrac * diff + value0;
			samples.setD(index, lerp);
			frac++;
			if (++index >= sampleCount) return;
		}
		double[] scratch = new double[ALIGNMENT];
		while (true) {
			//check for next cube.
			if (frac >= scale) {
				value0 = value1;
				value1 = cornerGetter.get(++cornerPos) * amplitude;
				diff = value1 - value0;
				frac = 0;
			}

			//vector ops.
			for (int i = 0; i < ALIGNMENT; i++) {
				scratch[i] = (frac + i) * rcp;
			}
			for (int i = 0; i < ALIGNMENT; i++) {
				scratch[i] = scratch[i] * scratch[i] * (scratch[i] * -2.0D + 3.0D);
			}
			for (int i = 0; i < ALIGNMENT; i++) {
				scratch[i] = scratch[i] * diff + value0;
			}

			//storage.
			for (int i = 0; i < ALIGNMENT; i++) {
				if (index + i >= sampleCount) return;
				samples.setD(index + i, scratch[i]);
			}

			index += ALIGNMENT;
			frac += ALIGNMENT;
		}
	}

	@Override
	public void getValuesY_XZ(long seed, int x, int startY, int z, double fracX, double fracZ, NumberArray samples) {
		if ((this.scaleY & (ALIGNMENT - 1)) == 0) {
			int relativeX = Math.floorDiv(x, this.scaleX);
			int relativeZ = Math.floorDiv(z, this.scaleZ);
			this.vectorizedLoopY4(startY, (int relativeY) -> this.getValue_XZ(seed, relativeX, relativeY, relativeZ, fracX, fracZ), samples);
		}
		else {
			super.getValuesY_XZ(seed, x, startY, z, fracX, fracZ, samples);
		}
	}
	*/

	@Override
	public double fracX(int fracX) {
		return Interpolator.smooth(fracX * this.rcpX);
	}

	@Override
	public double fracY(int fracY) {
		return Interpolator.smooth(fracY * this.rcpY);
	}

	@Override
	public double fracZ(int fracZ) {
		return Interpolator.smooth(fracZ * this.rcpZ);
	}
}