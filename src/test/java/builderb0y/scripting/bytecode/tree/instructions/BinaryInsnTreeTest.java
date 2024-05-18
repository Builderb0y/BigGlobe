package builderb0y.scripting.bytecode.tree.instructions;

import java.util.function.Consumer;

import org.apache.commons.lang3.function.FailableSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.opentest4j.AssertionFailedError;

import builderb0y.scripting.ScriptInterfaces.*;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("all")
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
	public static final Consumer<MutableScriptEnvironment> SPECIAL_FLOATS = (MutableScriptEnvironment environment) -> {
		environment.addVariableConstant("inf", Double.POSITIVE_INFINITY).addVariableConstant("nan", Double.NaN);
	};

	@Test
	public void testIntInt() throws ScriptParsingException {
		System.out.println("TESTING INT INT");
		for (String operator : INT_INT_OPERATORS) {
			System.out.println(operator);
			IntBinaryOperator varVar = new ScriptParser<>(IntBinaryOperator.class, "x " + operator + " y").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT).addVariableLoad("y", TypeInfos.INT)).parse(new ScriptClassLoader());
			for (int left : INTS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> {
						try {
							//if we're able to constant fold without overflow or precision loss...
							switch (operator) {
								case "+" -> Math.addExact(left, right);
								case "-" -> Math.subtractExact(left, right);
								case "*" -> Math.multiplyExact(left, right);
								case "/" -> {
									if (left / right * right != left) {
										throw new ArithmeticException();
									}
								}
							}
							//...then make sure the script parser can do so too.
							return new ScriptParser<>(IntSupplier.class, left + " " + operator + " " + right).parse(new ScriptClassLoader()).getAsInt();
						}
						catch (ArithmeticException ignored) {
							//otherwise, do the operation the lossy overflowy way,
							//and make sure the script parser gets the same result
							//when one of the operands isn't constant.
							return switch (operator) {
								case "+" -> left + right;
								case "-" -> left - right;
								case "*" -> left * right;
								case "/" -> Math.floorDiv(left, right);
								default -> throw new AssertionError(operator);
							};
						}
					});
					Object b = get(expectFail, () -> new ScriptParser<>(IntUnaryOperator.class, "x " + operator + " " + right).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader()).applyAsInt(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntUnaryOperator.class, left + " " + operator + " x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader()).applyAsInt(right));
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
			LongBinaryOperator varVar = new ScriptParser<>(LongBinaryOperator.class, "x " + operator + " y").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG).addVariableLoad("y", TypeInfos.LONG)).parse(new ScriptClassLoader());
			for (long left : LONGS) {
				for (long right : LONGS) {
					boolean expectFail = (
						(operator == "/" && right == 0L) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> {
						try {
							//if we're able to constant fold without overflow or precision loss...
							switch (operator) {
								case "+" -> Math.addExact(left, right);
								case "-" -> Math.subtractExact(left, right);
								case "*" -> Math.multiplyExact(left, right);
								case "/" -> {
									if (left / right * right != left) {
										throw new ArithmeticException();
									}
								}
							}
							//...then make sure the script parser can do so too.
							return new ScriptParser<>(LongSupplier.class, left + "L " + operator + " " + right + "L").parse(new ScriptClassLoader()).getAsLong();
						}
						catch (ArithmeticException ignored) {
							//otherwise, do the operation the lossy overflowy way,
							//and make sure the script parser gets the same result
							//when one of the operands isn't constant.
							return switch (operator) {
								case "+" -> left + right;
								case "-" -> left - right;
								case "*" -> left * right;
								case "/" -> Math.floorDiv(left, right);
								default -> throw new AssertionError(operator);
							};
						}
					});
					Object b = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, "x " + operator + " " + right + "L").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG)).parse(new ScriptClassLoader()).applyAsLong(left));
					Object c = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, left + "L " + operator + " x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG)).parse(new ScriptClassLoader()).applyAsLong(right));
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
			LongIntToLongOperator varVar = new ScriptParser<>(LongIntToLongOperator.class, "x " + operator + " y").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG).addVariableLoad("y", TypeInfos.INT)).parse(new ScriptClassLoader());
			for (long left : LONGS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(LongSupplier.class, left + "L " + operator + " " + right).parse(new ScriptClassLoader()).getAsLong());
					Object b = get(expectFail, () -> new ScriptParser<>(LongUnaryOperator.class, "x " + operator + " " + right).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG)).parse(new ScriptClassLoader()).applyAsLong(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToLongOperator.class, left + "L " + operator + " x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader()).applyAsLong(right));
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
			FloatBinaryOperator varVar = new ScriptParser<>(FloatBinaryOperator.class, "x " + operator + " y").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.FLOAT).addVariableLoad("y", TypeInfos.FLOAT)).parse(new ScriptClassLoader());
			for (float left : FLOATS) {
				for (float right : FLOATS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(FloatSupplier.class, FLOAT_FORMAT.format(left) + " " + operator + " " + FLOAT_FORMAT.format(right)).configureEnvironment(SPECIAL_FLOATS).parse(new ScriptClassLoader()).getAsFloat());
					Object b = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, "x " + operator + " " + FLOAT_FORMAT.format(right)).configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.FLOAT)).parse(new ScriptClassLoader()).applyAsFloat(left));
					Object c = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, FLOAT_FORMAT.format(left) + " " + operator + " x").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.FLOAT)).parse(new ScriptClassLoader()).applyAsFloat(right));
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
			FloatIntToFloatOperator varVar = new ScriptParser<>(FloatIntToFloatOperator.class, "x " + operator + " y").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.FLOAT).addVariableLoad("y", TypeInfos.INT)).parse(new ScriptClassLoader());
			for (float left : FLOATS) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(FloatSupplier.class, FLOAT_FORMAT.format(left) + " " + operator + " " + right).configureEnvironment(SPECIAL_FLOATS).parse(new ScriptClassLoader()).getAsFloat());
					Object b = get(expectFail, () -> new ScriptParser<>(FloatUnaryOperator.class, "x " + operator + " " + right).configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x",TypeInfos.FLOAT)).parse(new ScriptClassLoader()).applyAsFloat(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToFloatOperator.class, FLOAT_FORMAT.format(left) + " " + operator + " x").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader()).applyAsFloat(right));
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
			DoubleBinaryOperator varVar = new ScriptParser<>(DoubleBinaryOperator.class, "x " + operator + " y").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE).addVariableLoad("y", TypeInfos.DOUBLE)).parse(new ScriptClassLoader());
			for (double left : DOUBLES) {
				for (double right : DOUBLES) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(DoubleSupplier.class, DOUBLE_FORMAT.format(left) + " " + operator + " " + DOUBLE_FORMAT.format(right)).configureEnvironment(SPECIAL_FLOATS).parse(new ScriptClassLoader()).getAsDouble());
					Object b = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, "x " + operator + " " + DOUBLE_FORMAT.format(right)).configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE)).parse(new ScriptClassLoader()).applyAsDouble(left));
					Object c = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, DOUBLE_FORMAT.format(left) + " " + operator + " x").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE)).parse(new ScriptClassLoader()).applyAsDouble(right));
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
			DoubleIntToDoubleOperator varVar = new ScriptParser<>(DoubleIntToDoubleOperator.class, "x " + operator + " y").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE).addVariableLoad("y", TypeInfos.INT)).parse(new ScriptClassLoader());
			for (double left : DOUBLES) {
				for (int right : INTS) {
					boolean expectFail = (
						(operator == "/" && right == 0) ||
						(operator == "^" && left == 0 && right < 0)
					);
					Object a = get(expectFail, () -> new ScriptParser<>(DoubleSupplier.class, DOUBLE_FORMAT.format(left) + " " + operator + " " + right).configureEnvironment(SPECIAL_FLOATS).parse(new ScriptClassLoader()).getAsDouble());
					Object b = get(expectFail, () -> new ScriptParser<>(DoubleUnaryOperator.class, "x " + operator + " " + right).configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE)).parse(new ScriptClassLoader()).applyAsDouble(left));
					Object c = get(expectFail, () -> new ScriptParser<>(IntToDoubleOperator.class, DOUBLE_FORMAT.format(left) + " " + operator + " x").configureEnvironment(SPECIAL_FLOATS).configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader()).applyAsDouble(right));
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

	public static float toFinite(float f) {
		if (f == Float.NEGATIVE_INFINITY) return -Float.MAX_VALUE;
		if (f == Float.POSITIVE_INFINITY) return  Float.MAX_VALUE;
		return f;
	}

	public static double toFinite(double d) {
		if (d == Double.NEGATIVE_INFINITY) return -Double.MAX_VALUE;
		if (d == Double.POSITIVE_INFINITY) return  Double.MAX_VALUE;
		return d;
	}

	public static void assertSimilar(String operator, Object a, Object b) {
		if (operator == "^") {
			if (a instanceof Float af && b instanceof Float bf) {
				if (Float.floatToIntBits(af) == Float.floatToIntBits(bf)) return;
				float diff = toFinite(af) / toFinite(bf);
				assertEquals(1.0F, diff, 0.00001F);
			}
			else if (a instanceof Double ad && b instanceof Double bd) {
				if (Double.doubleToLongBits(ad) == Double.doubleToLongBits(bd)) return;
				double diff = toFinite(ad) / toFinite(bd);
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
			if (a.getClass() != b.getClass()) {
				if (a instanceof Throwable throwable) {
					System.err.println("a:");
					throwable.printStackTrace();
				}
				if (b instanceof Throwable throwable) {
					System.out.println("b:");
					throwable.printStackTrace();
				}
				throw new AssertionFailedError(null, a, b);
			}
		}
		else {
			assertEquals(a, b);
		}
	}
}