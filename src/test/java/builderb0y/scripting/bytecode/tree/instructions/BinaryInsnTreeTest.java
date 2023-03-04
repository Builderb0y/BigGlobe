package builderb0y.scripting.bytecode.tree.instructions;

import java.util.function.*;

import org.apache.commons.lang3.function.FailableSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class BinaryInsnTreeTest extends OperatorTest {

	public static final String[] INT_INT_OPERATORS = {
		"^", "+", "-", "*", "/", "%", "<<", "<<<", ">>", ">>>", "&", "|", "#"
	};
	public static final String[] LONG_LONG_OPERATORS = {
		"+", "-", "*", "/", "%", "&", "|", "#"
	};
	public static final String[] LONG_INT_OPERATORS = {
		"^", "<<", "<<<", ">>", ">>>"
	};
	public static final String[] FLOAT_FLOAT_OPERATORS = {
		"^", "+", "-", "*", "/", "%"
	};
	public static final String[] FLOAT_INT_OPERATORS = {
		"^", "<<", ">>"
	};
	public static final String[] DOUBLE_DOUBLE_OPERATORS = {
		"^", "+", "-", "*", "/", "%"
	};
	public static final String[] DOUBLE_INT_OPERATORS = {
		"^", "<<", ">>"
	};

	@Test
	public void testIntInt() throws ScriptParsingException {
		System.out.println("TESTING INT INT");
		for (String operator : INT_INT_OPERATORS) {
			System.out.println(operator);
			IntBinaryOperator varVar = new ScriptParser<>(IntBinaryOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.INT).addParameter("y", 2, TypeInfos.INT)).parse();
			for (int left : INTS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(IntSupplier.class, left + " " + operator + " " + right).parse().getAsInt());
					Object b = get(expectFail, () -> new ScriptParser<>(IntUnaryOperator.class, "x " + operator + " " + right).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.INT)).parse().applyAsInt(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntUnaryOperator.class, left + " " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.INT)).parse().applyAsInt(right));
					Object d = get(expectFail, () -> varVar.applyAsInt(left, right));
					assertExceptionEquals(a, b);
					assertExceptionEquals(a, c);
					assertExceptionEquals(a, d);
				}
			}
		}
	}

	@Test
	public void testLongLong() throws ScriptParsingException {
		System.out.println("TESTING LONG LONG");
		for (String operator : LONG_LONG_OPERATORS) {
			System.out.println(operator);
			LongBinaryOperator varVar = new ScriptParser<>(LongBinaryOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.LONG).addParameter("y", 3, TypeInfos.LONG)).parse();
			for (long left : LONGS) {
				for (long right : LONGS) {
					boolean expectFail = (
						(operator == "/" && right == 0L) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(LongSupplier.class, left + "L " + operator + " " + right + "L").parse().getAsLong());
					Object b = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, "x " + operator + " " + right + "L").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.LONG)).parse().applyAsLong(left));
					Object c = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, left + "L " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.LONG)).parse().applyAsLong(right));
					Object d = get(expectFail, () -> varVar.applyAsLong(left, right));
					assertExceptionEquals(a, b);
					assertExceptionEquals(a, c);
					assertExceptionEquals(a, d);
				}
			}
		}
	}

	@Test
	public void testLongInt() throws ScriptParsingException {
		System.out.println("TESTING LONG INT");
		for (String operator : LONG_INT_OPERATORS) {
			System.out.println(operator);
			LongIntToLongOperator varVar = new ScriptParser<>(LongIntToLongOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.LONG).addParameter("y", 3, TypeInfos.INT)).parse();
			for (long left : LONGS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(LongSupplier.class, left + "L " + operator + " " + right).parse().getAsLong());
					Object b = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, "x " + operator + " " + right).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.LONG)).parse().applyAsLong(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToLongFunction.class, left + "L " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.INT)).parse().applyAsLong(right));
					Object d = get(expectFail, () -> varVar.applyAsLong(left, right));
					assertExceptionEquals(a, b);
					assertExceptionEquals(a, c);
					assertExceptionEquals(a, d);
				}
			}
		}
	}

	@Test
	public void testFloatFloat() throws ScriptParsingException {
		System.out.println("TESTING FLOAT FLOAT");
		for (String operator : FLOAT_FLOAT_OPERATORS) {
			System.out.println(operator);
			FloatBinaryOperator varVar = new ScriptParser<>(FloatBinaryOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.FLOAT).addParameter("y", 2, TypeInfos.FLOAT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse();
			for (float left : FLOATS) {
				for (float right : FLOATS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(FloatSupplier.class, FLOAT_FORMAT.format(left) + " " + operator + " " + FLOAT_FORMAT.format(right)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsFloat());
					Object b = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, "x " + operator + " " + FLOAT_FORMAT.format(right)).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.FLOAT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsFloat(left));
					Object c = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, FLOAT_FORMAT.format(left) + " " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.FLOAT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsFloat(right));
					Object d = get(expectFail, () -> varVar.applyAsFloat(left, right));
					assertSimilar(operator, a, b);
					assertSimilar(operator, a, c);
					assertSimilar(operator, a, d);
				}
			}
		}
	}

	@Test
	public void testFloatInt() throws ScriptParsingException {
		System.out.println("TESTING FLOAT INT");
		for (String operator : FLOAT_INT_OPERATORS) {
			System.out.println(operator);
			FloatIntToFloatOperator varVar = new ScriptParser<>(FloatIntToFloatOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.FLOAT).addParameter("y", 2, TypeInfos.INT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse();
			for (float left : FLOATS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(FloatSupplier.class, FLOAT_FORMAT.format(left) + " " + operator + " " + right).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsFloat());
					Object b = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, "x " + operator + " " + right).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.FLOAT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsFloat(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToFloatOperator.class, FLOAT_FORMAT.format(left) + " " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.INT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsFloat(right));
					Object d = get(expectFail, () -> varVar.applyAsFloat(left, right));
					assertSimilar(operator, a, b);
					assertSimilar(operator, a, c);
					assertSimilar(operator, a, d);
				}
			}
		}
	}

	@Test
	public void testDoubleDouble() throws ScriptParsingException {
		System.out.println("TESTING DOUBLE DOUBLE");
		for (String operator : DOUBLE_DOUBLE_OPERATORS) {
			System.out.println(operator);
			DoubleBinaryOperator varVar = new ScriptParser<>(DoubleBinaryOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.DOUBLE).addParameter("y", 3, TypeInfos.DOUBLE)).addEnvironment(MathScriptEnvironment.INSTANCE).parse();
			for (double left : DOUBLES) {
				for (double right : DOUBLES) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(DoubleSupplier.class, DOUBLE_FORMAT.format(left) + " " + operator + " " + DOUBLE_FORMAT.format(right)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsDouble());
					Object b = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, "x " + operator + " " + DOUBLE_FORMAT.format(right)).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.DOUBLE)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsDouble(left));
					Object c = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, DOUBLE_FORMAT.format(left) + " " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.DOUBLE)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsDouble(right));
					Object d = get(expectFail, () -> varVar.applyAsDouble(left, right));
					assertSimilar(operator, a, b);
					assertSimilar(operator, a, c);
					assertSimilar(operator, a, d);
				}
			}
		}
	}

	@Test
	public void testDoubleInt() throws ScriptParsingException {
		System.out.println("TESTING DOUBLE INT");
		for (String operator : DOUBLE_INT_OPERATORS) {
			System.out.println(operator);
			DoubleIntToDoubleOperator varVar = new ScriptParser<>(DoubleIntToDoubleOperator.class, "x " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.DOUBLE).addParameter("y", 3, TypeInfos.INT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse();
			for (double left : DOUBLES) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(DoubleSupplier.class, DOUBLE_FORMAT.format(left) + " " + operator + " " + right).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsDouble());
					Object b = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, "x " + operator + " " + right).addEnvironment(new MutableScriptEnvironment().addParameter("x", 1, TypeInfos.DOUBLE)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsDouble(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToDoubleFunction.class, DOUBLE_FORMAT.format(left) + " " + operator + " y").addEnvironment(new MutableScriptEnvironment().addParameter("y", 1, TypeInfos.INT)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().applyAsDouble(right));
					Object d = get(expectFail, () -> varVar.applyAsDouble(left, right));
					assertSimilar(operator, a, b);
					assertSimilar(operator, a, c);
					assertSimilar(operator, a, d);
				}
			}
		}
	}

	public static Object get(boolean expectFail, FailableSupplier<?, Throwable> supplier) throws ScriptParsingException {
		try {
			return supplier.get();
		}
		catch (ScriptParsingException exception) {
			if (expectFail) {
				if (exception.getCause() == null) {
					return exception;
				}
				return exception.getCause();
			}
			else {
				throw exception;
			}
		}
		catch (Throwable exception) {
			return exception;
		}
	}

	public static void assertSimilar(String operator, Object a, Object b) {
		if (operator == "^") {
			if (a instanceof Float af && b instanceof Float bf) {
				if (Float.floatToIntBits(af) == Float.floatToIntBits(bf)) return;
				float diff = af / bf;
				assertEquals(1.0F, diff, 0.0000001F);
			}
			else if (a instanceof Double ad && b instanceof Double bd) {
				if (Double.doubleToLongBits(ad) == Double.doubleToLongBits(bd)) return;
				double diff = ad / bd;
				assertEquals(1.0D, diff, 0.0000000000001D);
			}
			else {
				assertExceptionEquals(a, b);
			}
		}
		else {
			assertExceptionEquals(a, b);
		}
	}

	public static void assertExceptionEquals(Object a, Object b) {
		if (a == null || b == null) {
			assertSame(a, b);
		}
		else if (a instanceof Throwable || b instanceof Throwable) {
			assertSame(a.getClass(), b.getClass());
		}
		else {
			assertEquals(a, b);
		}
	}

	@FunctionalInterface
	public static interface LongIntToLongOperator {

		public abstract long applyAsLong(long left, int right);
	}

	@FunctionalInterface
	public static interface FloatSupplier {

		public abstract float getAsFloat();
	}

	@FunctionalInterface
	public static interface FloatUnaryOperator {

		public abstract float applyAsFloat(float operand);
	}

	@FunctionalInterface
	public static interface FloatBinaryOperator {

		public abstract float applyAsFloat(float left, float right);
	}

	@FunctionalInterface
	public static interface IntToFloatOperator {

		public abstract float applyAsFloat(int operand);
	}

	@FunctionalInterface
	public static interface FloatIntToFloatOperator {

		public abstract float applyAsFloat(float left, int right);
	}

	@FunctionalInterface
	public static interface DoubleIntToDoubleOperator {

		public abstract double applyAsDouble(double left, int right);
	}
}