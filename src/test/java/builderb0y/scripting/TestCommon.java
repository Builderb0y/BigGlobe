package builderb0y.scripting;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Printer;
import org.opentest4j.AssertionFailedError;

import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class TestCommon {

	public static void assertSuccess(Object expected, String script) throws ScriptParsingException {
		Object actual = evaluate(script);
		if (expected instanceof Number a && actual instanceof Number b) {
			assertEquals(a.doubleValue(), b.doubleValue());
		}
		else {
			assertEquals(expected, actual);
		}
	}

	public static void assertFail(String message, String script) throws AssertionError {
		try {
			fail(String.valueOf(evaluate(script)));
		}
		catch (ScriptParsingException expected) {
			assertEquals(message, expected.getMessage());
		}
	}

	public static Object evaluate(String input) throws ScriptParsingException {
		return (
			new ScriptParser<>(Supplier.class, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(JavaUtilScriptEnvironment.ALL)
			.parse()
			.get()
		);
	}

	public static void assertOpcodes(String input, Class<?> implementationClass, int... expectedOpcodes) throws ScriptParsingException {
		ScriptParser<?> parser = (
			new ScriptParser<>(implementationClass, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(JavaUtilScriptEnvironment.ALL)
		);
		parser.toBytecode();
		int[] actualOpcodes = (
			StreamSupport
			.stream(parser.method.node.instructions.spliterator(), false)
			.mapToInt(AbstractInsnNode::getOpcode)
			.filter((int opcode) -> opcode != -1)
			.toArray()
		);
		if (!Arrays.equals(expectedOpcodes, actualOpcodes)) {
			throw new AssertionFailedError("Incorrect opcodes", opcodesToString(expectedOpcodes), opcodesToString(actualOpcodes));
		}
	}

	public static String opcodesToString(int... opcodes) {
		return Arrays.stream(opcodes).filter((int opcode) -> opcode != -1).mapToObj((int opcode) -> Printer.OPCODES[opcode]).collect(Collectors.joining(" "));
	}
}