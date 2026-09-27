package builderb0y.bigglobe.noise.polynomials;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

import builderb0y.bigglobe.BigGlobeMod;

/**
evaluates {@link SmoothPolynomial#interpolate(double)} for several fractions at once.
uses the exact same operations in the exact same order as the scalar version,
so the results are exactly the same too.

jdk.incubator.vector is only available when java is started with
--add-modules jdk.incubator.vector, so this class must not be loaded
unless {@link #AVAILABLE} is true.
*/
public class VectorizedSmoothPolynomial {

	public static final boolean AVAILABLE = ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent();
	static {
		if (AVAILABLE) BigGlobeMod.LOGGER.info("Using jdk.incubator.vector for smooth noise.");
	}

	public static class Impl {

		public static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
		public static final DoubleVector IOTA;
		static {
			double[] iota = new double[SPECIES.length()];
			for (int lane = 0; lane < iota.length; lane++) iota[lane] = lane;
			IOTA = DoubleVector.fromArray(SPECIES, iota, 0);
		}

		/**
		stores polynomial.interpolate((startMod + i) * rcp) in out[offset + i] for every i in [0, count).
		*/
		public static void interpolate(SmoothPolynomial polynomial, int startMod, double rcp, double[] out, int offset, int count) {
			double term0 = polynomial.term0, term1 = polynomial.term1, value0 = polynomial.value0;
			int lanes = SPECIES.length();
			int i = 0;
			for (int bound = count - lanes; i <= bound; i += lanes) {
				DoubleVector fraction = IOTA.add(startMod + i).mul(rcp);
				fraction.mul(term0).add(term1).mul(fraction).mul(fraction).add(value0).intoArray(out, offset + i);
			}
			for (; i < count; i++) {
				out[offset + i] = polynomial.interpolate((startMod + i) * rcp);
			}
		}
	}
}
