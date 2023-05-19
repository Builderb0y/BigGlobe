package builderb0y.scripting.bytecode.tree.flow.compare;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongToIntFunction;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;
import static org.junit.jupiter.api.Assertions.*;

public class CompareInsnTreeTest {

	public static final int[] INTS = { -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5 };
	public static final long[] LONGS = { -5L, -4L, -3L, -2L, -1L, 0L, 1L, 2L, 3L, 4L, 5L };
	public static final float[] FLOATS = { -5.0F, -4.0F, -3.0F, -2.0F, -1.0F, -0.0F, 0.0F, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };
	public static final double[] DOUBLES = { -5.0D, -4.0D, -3.0D, -2.0D, -1.0D, -0.0D, 0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
	public static final Numbers[] NUMBERS = { Numbers.ONE, Numbers.TWO, Numbers.THREE, Numbers.FOUR, Numbers.FIVE, null };

	@Test
	public void testInt() throws ScriptParsingException {
		IntBinaryOperator operator = (
			new ScriptParser<>(
				IntBinaryOperator.class,
				"""
				compare (x, y:
					case (>: 3)
					case (=: 2)
					case (<: 1)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.INT)
				.addVariableLoad("y", 2, TypeInfos.INT)
			)
			.parse()
		);
		for (int left : INTS) {
			for (int right : INTS) {
				assertEquals(left > right ? 3 : left == right ? 2 : 1, operator.applyAsInt(left, right));
			}
		}
	}

	@Test
	public void testZeroInt() throws ScriptParsingException {
		IntUnaryOperator operator = (
			new ScriptParser<>(
				IntUnaryOperator.class,
				"""
				compare (x:
					case (>: 3)
					case (=: 2)
					case (<: 1)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.INT)
			)
			.parse()
		);
		for (int left : INTS) {
			assertEquals(left > 0 ? 3 : left == 0 ? 2 : 1, operator.applyAsInt(left));
		}
	}

	@Test
	public void testLong() throws ScriptParsingException {
		LongComparator operator = (
			new ScriptParser<>(
				LongComparator.class,
				"""
				compare (x, y:
					case (>: 3)
					case (=: 2)
					case (<: 1)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.LONG)
				.addVariableLoad("y", 3, TypeInfos.LONG)
			)
			.parse()
		);
		for (long left : LONGS) {
			for (long right : LONGS) {
				assertEquals(left > right ? 3 : left == right ? 2 : 1, operator.applyAsLong(left, right));
			}
		}
	}

	@FunctionalInterface
	public static interface LongComparator {

		public abstract int applyAsLong(long a, long b);
	}

	@Test
	public void testZeroLong() throws ScriptParsingException {
		LongToIntFunction operator = (
			new ScriptParser<>(
				LongToIntFunction.class,
				"""
				compare (x:
					case (>: 3)
					case (=: 2)
					case (<: 1)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.LONG)
			)
			.parse()
		);
		for (long left : LONGS) {
			assertEquals(left > 0 ? 3 : left == 0 ? 2 : 1, operator.applyAsInt(left));
		}
	}

	@Test
	public void testFloat() throws ScriptParsingException {
		FloatComparator operator = (
			new ScriptParser<>(
				FloatComparator.class,
				"""
				compare (x, y:
					case (>: 3)
					case (=: 2)
					case (<: 1)
					case (!: 0)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.FLOAT)
				.addVariableLoad("y", 2, TypeInfos.FLOAT)
			)
			.parse()
		);
		for (float left : FLOATS) {
			for (float right : FLOATS) {
				assertEquals(left > right ? 3 : left == right ? 2 : left < right ? 1 : 0, operator.applyAsFloat(left, right));
			}
		}
	}

	@FunctionalInterface
	public static interface FloatComparator {

		public abstract int applyAsFloat(float a, float b);
	}

	@Test
	public void testZeroFloat() throws ScriptParsingException {
		FloatZeroComparator operator = (
			new ScriptParser<>(
				FloatZeroComparator.class,
				"""
				compare (x:
					case (>: 3)
					case (=: 2)
					case (<: 1)
					case (!: 0)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.FLOAT)
			)
			.parse()
		);
		for (float left : FLOATS) {
			assertEquals(left > 0 ? 3 : left == 0 ? 2 : left < 0 ? 1 : 0, operator.applyAsFloat(left));
		}
	}

	@FunctionalInterface
	public static interface FloatZeroComparator {

		public abstract int applyAsFloat(float a);
	}

	@Test
	public void testDouble() throws ScriptParsingException {
		DoubleComparator operator = (
			new ScriptParser<>(
				DoubleComparator.class,
				"""
				compare (x, y:
					case (>: 3)
					case (=: 2)
					case (<: 1)
					case (!: 0)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.DOUBLE)
				.addVariableLoad("y", 3, TypeInfos.DOUBLE)
			)
			.parse()
		);
		for (double left : DOUBLES) {
			for (double right : DOUBLES) {
				assertEquals(left > right ? 3 : left == right ? 2 : left < right ? 1 : 0, operator.applyAsDouble(left, right));
			}
		}
	}

	@FunctionalInterface
	public static interface DoubleComparator {

		public abstract int applyAsDouble(double a, double b);
	}

	@Test
	public void testZeroDouble() throws ScriptParsingException {
		DoubleZeroComparator operator = (
			new ScriptParser<>(
				DoubleZeroComparator.class,
				"""
				compare (x:
					case (>: 3)
					case (=: 2)
					case (<: 1)
					case (!: 0)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, TypeInfos.DOUBLE)
			)
			.parse()
		);
		for (double left : DOUBLES) {
			assertEquals(left > 0 ? 3 : left == 0 ? 2 : left < 0 ? 1 : 0, operator.applyAsDouble(left));
		}
	}

	@FunctionalInterface
	public static interface DoubleZeroComparator {

		public abstract int applyAsDouble(double a);
	}

	@Test
	public void testComparables() throws ScriptParsingException {
		NumbersComparator operator = (
			new ScriptParser<>(
				NumbersComparator.class,
				"""
				compare (x, y:
					case (>: 3)
					case (=: 2)
					case (<: 1)
					case (!: 0)
				)
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("x", 1, type(Numbers.class))
				.addVariableLoad("y", 2, type(Numbers.class))
			)
			.parse()
		);
		for (Numbers left : NUMBERS) {
			for (Numbers right : NUMBERS) {
				assertEquals(left == null || right == null ? 0 : left.compareTo(right) > 0 ? 3 : left.compareTo(right) == 0 ? 2 : 1, operator.applyAsNumber(left, right));
			}
		}
	}

	public static interface NumbersComparator {

		public abstract int applyAsNumber(Numbers left, Numbers right);
	}

	public static enum Numbers {
		ONE,
		TWO,
		THREE,
		FOUR,
		FIVE;
	}
}