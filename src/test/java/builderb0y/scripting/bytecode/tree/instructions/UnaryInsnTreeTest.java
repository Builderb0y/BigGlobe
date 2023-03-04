package builderb0y.scripting.bytecode.tree.instructions;

import java.util.function.*;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment2;
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
			IntUnaryOperator var = new ScriptParser<>(IntUnaryOperator.class, operator + " x").addEnvironment(new MutableScriptEnvironment2().addVariableLoad("x", 1, TypeInfos.INT)).parse();
			for (int operand : INTS) {
				int a = var.applyAsInt(operand);
				int b = new ScriptParser<>(IntSupplier.class, operator + " " + operand).parse().getAsInt();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testLong() throws ScriptParsingException {
		System.out.println("TESTING LONG");
		for (String operator : LONG_OPERATORS) {
			System.out.println(operator);
			LongUnaryOperator var = new ScriptParser<>(LongUnaryOperator.class, operator + " x").addEnvironment(new MutableScriptEnvironment2().addVariableLoad("x", 1, TypeInfos.LONG)).parse();
			for (long operand : LONGS) {
				long a = var.applyAsLong(operand);
				long b = new ScriptParser<>(LongSupplier.class, operator + " " + operand + "L").parse().getAsLong();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testFloat() throws ScriptParsingException {
		System.out.println("TESTING FLOAT");
		for (String operator : FLOAT_OPERATORS) {
			System.out.println(operator);
			FloatUnaryOperator var = new ScriptParser<>(FloatUnaryOperator.class, operator + "x").addEnvironment(new MutableScriptEnvironment2().addVariableLoad("x", 1, TypeInfos.FLOAT)).parse();
			for (float operand : FLOATS) {
				float a = var.applyAsFloat(operand);
				float b = new ScriptParser<>(FloatSupplier.class, operator + FLOAT_FORMAT.format(operand)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsFloat();
				assertEquals(a, b);
			}
		}
	}

	@Test
	public void testDouble() throws ScriptParsingException {
		System.out.println("TESTING DOUBLE");
		for (String operator : DOUBLE_OPERATORS) {
			System.out.println(operator);
			DoubleUnaryOperator var = new ScriptParser<>(DoubleUnaryOperator.class, operator + "x").addEnvironment(new MutableScriptEnvironment2().addVariableLoad("x", 1, TypeInfos.DOUBLE)).parse();
			for (double operand : DOUBLES) {
				double a = var.applyAsDouble(operand);
				double b = new ScriptParser<>(DoubleSupplier.class, operator + DOUBLE_FORMAT.format(operand)).addEnvironment(MathScriptEnvironment.INSTANCE).parse().getAsDouble();
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

		public abstract float applyAsFloat(float operand);
	}
}