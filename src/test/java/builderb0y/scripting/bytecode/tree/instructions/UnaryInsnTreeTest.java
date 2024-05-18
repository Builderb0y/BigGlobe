package builderb0y.scripting.bytecode.tree.instructions;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.ScriptInterfaces.*;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.junit.jupiter.api.Assertions.*;

public class UnaryInsnTreeTest extends OperatorTest {

	public static final String[] INT_OPERATORS = {
		"+", "-"
	};
	public static final String[] LONG_OPERATORS = {
		"+", "-"
	};
	public static final String[] FLOAT_OPERATORS = {
		"+", "-"
	};
	public static final String[] DOUBLE_OPERATORS = {
		"+", "-"
	};

	@Test
	public void testInt() throws ScriptParsingException {
		System.out.println("TESTING INT");
		for (String operator : INT_OPERATORS) {
			System.out.println(operator);
			IntUnaryOperator var = new ScriptParser<>(IntUnaryOperator.class, operator + " x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.INT)).parse(new ScriptClassLoader());
			for (int operand : INTS) {
				int a = var.applyAsInt(operand);
				int b = new ScriptParser<>(IntSupplier.class, operator + " " + operand).parse(new ScriptClassLoader()).getAsInt();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testLong() throws ScriptParsingException {
		System.out.println("TESTING LONG");
		for (String operator : LONG_OPERATORS) {
			System.out.println(operator);
			LongUnaryOperator var = new ScriptParser<>(LongUnaryOperator.class, operator + " x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.LONG)).parse(new ScriptClassLoader());
			for (long operand : LONGS) {
				long a = var.applyAsLong(operand);
				long b = new ScriptParser<>(LongSupplier.class, operator + " " + operand + "L").parse(new ScriptClassLoader()).getAsLong();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testFloat() throws ScriptParsingException {
		System.out.println("TESTING FLOAT");
		for (String operator : FLOAT_OPERATORS) {
			System.out.println(operator);
			FloatUnaryOperator var = new ScriptParser<>(FloatUnaryOperator.class, operator + "x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.FLOAT)).parse(new ScriptClassLoader());
			for (float operand : FLOATS) {
				float a = var.applyAsFloat(operand);
				float b = new ScriptParser<>(FloatSupplier.class, operator + FLOAT_FORMAT.format(operand)).addEnvironment(MathScriptEnvironment.INSTANCE).parse(new ScriptClassLoader()).getAsFloat();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testDouble() throws ScriptParsingException {
		System.out.println("TESTING DOUBLE");
		for (String operator : DOUBLE_OPERATORS) {
			System.out.println(operator);
			DoubleUnaryOperator var = new ScriptParser<>(DoubleUnaryOperator.class, operator + "x").configureEnvironment(e -> e.addVariableLoad("x", TypeInfos.DOUBLE)).parse(new ScriptClassLoader());
			for (double operand : DOUBLES) {
				double a = var.applyAsDouble(operand);
				double b = new ScriptParser<>(DoubleSupplier.class, operator + DOUBLE_FORMAT.format(operand)).addEnvironment(MathScriptEnvironment.INSTANCE).parse(new ScriptClassLoader()).getAsDouble();
				assertEquals(a, b);
			}
		}
	}

	@FunctionalInterface
	public static interface FloatSupplier {

		public abstract float getAsFloat();
	}

	@FunctionalInterface
	public static interface FloatUnaryOperator {

		public abstract float applyAsFloat(float x);
	}
}