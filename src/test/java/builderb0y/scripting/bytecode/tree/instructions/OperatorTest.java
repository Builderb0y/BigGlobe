package builderb0y.scripting.bytecode.tree.instructions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

public class OperatorTest {

	public static final int[] INTS = {
		Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, Integer.MIN_VALUE + 3,
		-3, -2, -1, 0, 1, 2, 3,
		Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE
	};
	public static final long[] LONGS = {
		Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2, Long.MIN_VALUE + 3,
		Integer.MIN_VALUE - 5L, Integer.MIN_VALUE - 4L, Integer.MIN_VALUE - 3L,
		Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, Integer.MIN_VALUE + 3,
		-3, -2, -1, 0, 1, 2, 3,
		Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
		Integer.MAX_VALUE + 1L, Integer.MAX_VALUE + 2L, Integer.MAX_VALUE + 3L,
		Long.MAX_VALUE - 3L, Long.MAX_VALUE - 2L, Long.MAX_VALUE - 1L
	};
	public static final float[] FLOATS = andNegatives(
		0.0F,
		Float.MIN_VALUE, Float.MIN_VALUE * 2.0F, Float.MIN_VALUE * 3.0F,
		down(Float.MIN_NORMAL, 3), down(Float.MIN_NORMAL, 2), down(Float.MIN_NORMAL, 1),
		Float.MIN_NORMAL,
		up(Float.MIN_NORMAL, 1), up(Float.MIN_NORMAL, 2), up(Float.MIN_NORMAL, 3),
		Float.MIN_NORMAL * 2.0F, Float.MIN_NORMAL * 3.0F,
		down(1.0F, 3), down(1.0F, 2), down(1.0F, 1),
		up(1.0F, 1), up(1.0F, 2), up(1.0F, 3),
		0.25F, 0.5F, 0.75F, 1.0F,
		1.25F, 1.5F, 1.75F, 2.0F,
		3.0F,
		down(Float.MAX_VALUE, 3), down(Float.MAX_VALUE, 2), down(Float.MAX_VALUE, 1),
		Float.MAX_VALUE,
		Float.POSITIVE_INFINITY,
		Float.NaN
	);
	public static final double[] DOUBLES = andNegatives(
		0.0D,
		Double.MIN_VALUE, Double.MIN_VALUE * 2.0D, Double.MIN_VALUE * 3.0D,
		down(Double.MIN_NORMAL, 3), down(Double.MIN_NORMAL, 2), down(Double.MIN_NORMAL, 1),
		Double.MIN_NORMAL,
		up(Double.MIN_NORMAL, 1), up(Double.MIN_NORMAL, 2), up(Double.MIN_NORMAL, 3),
		Double.MIN_NORMAL * 2.0D, Double.MIN_NORMAL * 3.0D,
		down(1.0D, 3), down(1.0D, 2), down(1.0D, 1),
		up(1.0D, 1), up(1.0D, 2), up(1.0D, 3),
		0.25D, 0.5D, 0.75D, 1.0D,
		1.25D, 1.5D, 1.75D, 2.0D,
		3.0D,
		down(Double.MAX_VALUE, 3), down(Double.MAX_VALUE, 2), down(Double.MAX_VALUE, 1),
		Double.MAX_VALUE,
		Double.POSITIVE_INFINITY,
		Double.NaN
	);

	public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat();
	static {
		FLOAT_FORMAT.setMinimumFractionDigits(1);
		FLOAT_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
		FLOAT_FORMAT.setGroupingUsed(false);
		DecimalFormatSymbols symbols = FLOAT_FORMAT.getDecimalFormatSymbols();
		symbols.setNaN("float(nan)");
		symbols.setInfinity("float(inf)");
		FLOAT_FORMAT.setDecimalFormatSymbols(symbols);
		FLOAT_FORMAT.setPositivePrefix("float(");
		FLOAT_FORMAT.setPositiveSuffix(")");
		FLOAT_FORMAT.setNegativePrefix("float(-");
		FLOAT_FORMAT.setNegativeSuffix(")");
	}

	public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat();
	static {
		DOUBLE_FORMAT.setMinimumFractionDigits(1);
		DOUBLE_FORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
		DOUBLE_FORMAT.setGroupingUsed(false);
		DecimalFormatSymbols symbols = DOUBLE_FORMAT.getDecimalFormatSymbols();
		symbols.setNaN("double(nan)");
		symbols.setInfinity("double(inf)");
		DOUBLE_FORMAT.setDecimalFormatSymbols(symbols);
		DOUBLE_FORMAT.setPositivePrefix("double(");
		DOUBLE_FORMAT.setPositiveSuffix(")");
		DOUBLE_FORMAT.setNegativePrefix("double(-");
		DOUBLE_FORMAT.setNegativeSuffix(")");
	}

	public static float up(float f, int by) {
		while (--by >= 0) f = Math.nextUp(f);
		return f;
	}

	public static float down(float f, int by) {
		while (--by >= 0) f = Math.nextDown(f);
		return f;
	}

	public static double up(double f, int by) {
		while (--by >= 0) f = Math.nextUp(f);
		return f;
	}

	public static double down(double f, int by) {
		while (--by >= 0) f = Math.nextDown(f);
		return f;
	}

	public static float[] andNegatives(float... array) {
		float[] copy = Arrays.copyOf(array, array.length << 1);
		for (int index = 0, length = array.length; index < length; index++) {
			copy[index + length] = -array[index];
		}
		return copy;
	}

	public static double[] andNegatives(double... array) {
		double[] copy = Arrays.copyOf(array, array.length << 1);
		for (int index = 0, length = array.length; index < length; index++) {
			copy[index + length] = -array[index];
		}
		return copy;
	}
}