package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.math.Interpolator;

/**
a {@link Grid2D}-like class which produces noise with
sharp edges where the wrapped {@link #absGrid}'s value is 0.
however, it can also produce smooth, continuous values in these same conditions.
the sharpness of the edge in question is another parameter,
so it can vary with position. but because it's another parameter,
this class cannot directly implement {@link Grid2D}.

in practice, the sharpness parameter is controlled by another Grid.
I would've made this other grid a field on this Grid,
so that the sharpness can be queried as-needed
instead of being passed in as another parameter,
but the underlying grid which controls sharpness also controls other things,
and it would be inefficient to query this underlying grid multiple times.
*/
@AddPseudoField(name = "grid", getter = "getGrid")
public class ErosionGrid2D {

	public final transient AbsGrid2D absGrid;
	public final double amplitude;

	public ErosionGrid2D(double amplitude, Grid2D grid) {
		this.absGrid = new AbsGrid2D(grid);
		this.amplitude = amplitude;
	}

	public void getValueAndSnow(long seed, int x, int y, double sharpness, double[] out) {
		double value = this.absGrid.getValue(seed, x, y) / this.absGrid.maxValue(); //0 to 1.
		double curvedValue = value * Interpolator.mixLinear(value, 2.0D - value, sharpness);
		double offset = sharpness * 2.0D / Math.abs(this.amplitude);
		double snow = hyperbola(curvedValue, offset);
		curvedValue = curvedValue * -2.0D + 1.0D; //-1 to 1.
		curvedValue *= this.amplitude;
		snow = snow * -2.0D + 1.0D; //-1 to 1.
		snow *= this.amplitude;
		out[0] = curvedValue;
		out[1] = snow;
	}

	public static double hyperbola(double value, double offset) {
		return Math.sqrt(value * value + offset * offset);
	}

	public Grid2D getGrid() {
		return this.absGrid.grid;
	}
}