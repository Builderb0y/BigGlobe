package builderb0y.scripting.bytecode.tree.instructions;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class BetweenTest {

	public static final int[] INTS = { -2, -1, 0, 1, 2 };
	public static final long[] LONGS = { -2L, -1L, 0L, 1L, 2L };
	public static final float[] FLOATS = { Float.NEGATIVE_INFINITY, -2.0F, -1.0F, -0.0F, 0.0F, 1.0F, 2.0F, Float.POSITIVE_INFINITY, Float.NaN };
	public static final double[] DOUBLES = { Double.NEGATIVE_INFINITY, -2.0D, -1.0D, -0.0D, 0.0D, 1.0D, 2.0D, Double.POSITIVE_INFINITY, Double.NaN };
	public static final boolean[] INCLUSIVES = { true, false };

	@Test
	public void testInts() throws ScriptParsingException {
		for (int value : INTS) {
			for (int min : INTS) {
				for (boolean minInclusive : INCLUSIVES) {
					for (int max : INTS) {
						for (boolean maxInclusive : INCLUSIVES) {
							String expected = "int(" + value + ") " + (minInclusive ? ">=" : ">") + " int(" + min + ") && int(" + value + ") " + (maxInclusive ? "<=" : "<") + " int(" + max + ")";
							String actual = "int(" + value + ").isBetween" + (minInclusive ? '[' : '(') + "int(" + min + "), int(" + max + ")" + (maxInclusive ? ']' : ')');
							assertEquals(TestCommon.evaluate(expected), TestCommon.evaluate(actual));
						}
					}
				}
			}
		}
	}

	@Test
	public void testLongs() throws ScriptParsingException {
		for (long value : LONGS) {
			for (long min : LONGS) {
				for (boolean minInclusive : INCLUSIVES) {
					for (long max : LONGS) {
						for (boolean maxInclusive : INCLUSIVES) {
							String expected = "long(" + value + ") " + (minInclusive ? ">=" : ">") + " long(" + min + ") && long(" + value + ") " + (maxInclusive ? "<=" : "<") + " long(" + max + ")";
							String actual = "long(" + value + ").isBetween" + (minInclusive ? '[' : '(') + "long(" + min + "), long(" + max + ")" + (maxInclusive ? ']' : ')');
							assertEquals(TestCommon.evaluate(expected), TestCommon.evaluate(actual));
						}
					}
				}
			}
		}
	}

	@Test
	public void testFloats() throws ScriptParsingException {
		for (float value : FLOATS) {
			for (float min : FLOATS) {
				for (boolean minInclusive : INCLUSIVES) {
					for (float max : FLOATS) {
						for (boolean maxInclusive : INCLUSIVES) {
							String expected = OperatorTest.FLOAT_FORMAT.format(value) + " " + (minInclusive ? ">=" : ">") + " " + OperatorTest.FLOAT_FORMAT.format(min) + " && " + OperatorTest.FLOAT_FORMAT.format(value) + " " + (maxInclusive ? "<=" : "<") + " " + OperatorTest.FLOAT_FORMAT.format(max);
							String actual = OperatorTest.FLOAT_FORMAT.format(value) + ".isBetween" + (minInclusive ? '[' : '(') + OperatorTest.FLOAT_FORMAT.format(min) + ", " + OperatorTest.FLOAT_FORMAT.format(max) + (maxInclusive ? ']' : ')');
							assertEquals(TestCommon.evaluate(expected), TestCommon.evaluate(actual));
						}
					}
				}
			}
		}
	}

	@Test
	public void testDoubles() throws ScriptParsingException {
		for (double value : DOUBLES) {
			for (double min : DOUBLES) {
				for (boolean minInclusive : INCLUSIVES) {
					for (double max : DOUBLES) {
						for (boolean maxInclusive : INCLUSIVES) {
							String expected = OperatorTest.DOUBLE_FORMAT.format(value) + " " + (minInclusive ? ">=" : ">") + " " + OperatorTest.DOUBLE_FORMAT.format(min) + " && " + OperatorTest.DOUBLE_FORMAT.format(value) + " " + (maxInclusive ? "<=" : "<") + " " + OperatorTest.DOUBLE_FORMAT.format(max);
							String actual = OperatorTest.DOUBLE_FORMAT.format(value) + ".isBetween" + (minInclusive ? '[' : '(') + OperatorTest.DOUBLE_FORMAT.format(min) + ", " + OperatorTest.DOUBLE_FORMAT.format(max) + (maxInclusive ? ']' : ')');
							assertEquals(TestCommon.evaluate(expected), TestCommon.evaluate(actual));
						}
					}
				}
			}
		}
	}
}