package builderb0y.scripting.parsing;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;
import builderb0y.scripting.bytecode.tree.instructions.OperatorTest;

import static org.junit.jupiter.api.Assertions.*;

public class CastingTest extends TestCommon {

	public static final float[] FLOATS = {
		Float.NEGATIVE_INFINITY,
		Math.nextDown((float)(Long.MIN_VALUE)),
		(float)(Long.MIN_VALUE),
		Math.nextUp((float)(Long.MIN_VALUE)),
		Math.nextDown((float)(Integer.MIN_VALUE)),
		(float)(Integer.MIN_VALUE),
		Math.nextUp((float)(Integer.MIN_VALUE)),
		-2.0F, -1.75F, -1.5F, -1.25F, -1.0F, -0.75F, -0.5F, -0.25F, -0.0F,
		0.0F, 0.25F, 0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 1.75F, 2.0F,
		Math.nextDown(-(float)(Integer.MIN_VALUE)),
		-(float)(Integer.MIN_VALUE),
		Math.nextUp(-(float)(Integer.MIN_VALUE)),
		Math.nextDown(-(float)(Long.MIN_VALUE)),
		-(float)(Long.MIN_VALUE),
		Math.nextUp(-(float)(Long.MIN_VALUE)),
		Float.POSITIVE_INFINITY,
		Float.NaN
	};
	public static final double[] DOUBLES = {
		Double.NEGATIVE_INFINITY,
		Math.nextDown((double)(Long.MIN_VALUE)),
		(double)(Long.MIN_VALUE),
		Math.nextUp((double)(Long.MIN_VALUE)),
		Integer.MIN_VALUE - 1.0D,
		Integer.MIN_VALUE - 0.75D,
		Integer.MIN_VALUE - 0.5D,
		Integer.MIN_VALUE - 0.25D,
		Integer.MIN_VALUE,
		Integer.MIN_VALUE + 0.25D,
		Integer.MIN_VALUE + 0.5D,
		Integer.MIN_VALUE + 0.75D,
		Integer.MIN_VALUE + 1.0D,
		-2.0D, -1.75D, -1.5D, -1.25D, -1.0D, -0.75D, -0.5D, -0.25D, -0.0D,
		0.0D, 0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 1.75D, 2.0D,
		-((double)(Integer.MIN_VALUE)) - 1.0D,
		-((double)(Integer.MIN_VALUE)) - 0.75D,
		-((double)(Integer.MIN_VALUE)) - 0.5D,
		-((double)(Integer.MIN_VALUE)) - 0.25D,
		-((double)(Integer.MIN_VALUE)),
		-((double)(Integer.MIN_VALUE)) + 0.25D,
		-((double)(Integer.MIN_VALUE)) + 0.5D,
		-((double)(Integer.MIN_VALUE)) + 0.75D,
		-((double)(Integer.MIN_VALUE)) + 1.0D,
		Math.nextDown(-(double)(Long.MIN_VALUE)),
		-(double)(Long.MIN_VALUE),
		Math.nextUp(-(double)(Long.MIN_VALUE)),
		Double.POSITIVE_INFINITY,
		Double.NaN
	};

	@Test
	public void testCasting() throws ScriptParsingException {
		for (Rounder rounder : Rounder.VALUES) {
			for (float in : FLOATS) {
				assertSuccess(Float.isFinite(in) ? toInt(rounder.round(new BigDecimal(in))) : (int)(in), rounder.name().toLowerCase(Locale.ROOT) + "Int(" + OperatorTest.FLOAT_FORMAT.format(in) + ')');
				assertSuccess(Float.isFinite(in) ? toLong(rounder.round(new BigDecimal(in))) : (long)(in), rounder.name().toLowerCase(Locale.ROOT) + "Long(" + OperatorTest.FLOAT_FORMAT.format(in) + ')');
			}
			for (double in : DOUBLES) {
				assertSuccess(Double.isFinite(in) ? toInt(rounder.round(new BigDecimal(in))) : (int)(in), rounder.name().toLowerCase(Locale.ROOT) + "Int(" + OperatorTest.DOUBLE_FORMAT.format(in) + ')');
				assertSuccess(Double.isFinite(in) ? toLong(rounder.round(new BigDecimal(in))) : (long)(in), rounder.name().toLowerCase(Locale.ROOT) + "Long(" + OperatorTest.DOUBLE_FORMAT.format(in) + ')');
			}
		}
	}

	public static final BigDecimal
		INT_MIN = new BigDecimal(Integer.MIN_VALUE),
		INT_MAX = new BigDecimal(Integer.MAX_VALUE);

	public static int toInt(BigDecimal value) {
		if (value.compareTo(INT_MAX) >= 0) return Integer.MAX_VALUE;
		if (value.compareTo(INT_MIN) <= 0) return Integer.MIN_VALUE;
		return value.intValueExact();
	}

	public static final BigDecimal
		LONG_MIN = new BigDecimal(Long.MIN_VALUE),
		LONG_MAX = new BigDecimal(Long.MAX_VALUE);

	public static long toLong(BigDecimal value) {
		if (value.compareTo(LONG_MAX) >= 0) return Long.MAX_VALUE;
		if (value.compareTo(LONG_MIN) <= 0) return Long.MIN_VALUE;
		return value.longValueExact();
	}

	public static enum Rounder {
		FLOOR {

			@Override
			public BigDecimal round(BigDecimal value) {
				value = value.subtract(mod1(value));
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		},
		CEIL {

			@Override
			public BigDecimal round(BigDecimal value) {
				BigDecimal mod = mod1(value);
				value = value.subtract(mod);
				if (mod.signum() > 0) value = value.add(BigDecimal.ONE);
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		},
		LOWER {

			@Override
			public BigDecimal round(BigDecimal value) {
				BigDecimal mod = mod1(value);
				value = value.subtract(mod.signum() == 0 ? BigDecimal.ONE : mod);
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		},
		HIGHER {

			@Override
			public BigDecimal round(BigDecimal value) {
				BigDecimal mod = mod1(value);
				value = value.add(BigDecimal.ONE.subtract(mod));
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		},
		TRUNC {

			@Override
			public BigDecimal round(BigDecimal value) {
				value = value.subtract(value.remainder(BigDecimal.ONE));
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		},
		ROUND {

			@Override
			public BigDecimal round(BigDecimal value) {
				BigDecimal mod = mod1(value);
				value = value.subtract(mod);
				if (mod.compareTo(HALF) >= 0) value = value.add(BigDecimal.ONE);
				assertEquals(0, value.remainder(BigDecimal.ONE).signum());
				return value;
			}
		};

		public static final BigDecimal HALF = new BigDecimal("0.5");
		public static final Rounder[] VALUES = values();

		public abstract BigDecimal round(BigDecimal value);

		public static BigDecimal mod1(BigDecimal value) {
			//WHY DOES BIGDECIMAL NOT HAVE A MODULUS METHOD AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
			BigDecimal result = value.remainder(BigDecimal.ONE);
			if (result.signum() < 0) result = result.add(BigDecimal.ONE);
			return result;
		}
	}

	@Test
	public void testInstanceOf() throws ScriptParsingException {
		assertSuccess(true, "'hi' . is ( String )");
		assertSuccess(false, "null . is ( String )");
		assertSuccess(false, "'hi' . isnt ( String )");
		assertSuccess(true, "null . isnt ( String )");
		assertSuccess("5", "5 . as ( String )");
	}
}