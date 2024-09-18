package builderb0y.scripting;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Printer;
import org.opentest4j.AssertionFailedError;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.util.ThrowingRunnable;
import builderb0y.scripting.ScriptInterfaces.ObjectSupplier;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class TestCommon {

	public static void assertSuccessExactType(Object expected, String script) throws ScriptParsingException {
		assertEquals(expected, evaluate(script));
	}

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
		catch (Exception expected) {
			if (!expected.getMessage().startsWith(message)) {
				throw new AssertionFailedError(null, message, expected.getMessage());
			}
		}
	}

	public static Object evaluate(String input) throws ScriptParsingException {
		return (
			new ScriptParser<>(ObjectSupplier.class, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
			.parse(new ScriptClassLoader())
			.getAsObject()
		);
	}

	public static void assertOpcodes(String input, Class<?> implementationClass, int... expectedOpcodes) throws ScriptParsingException {
		ScriptParser<?> parser = (
			new ScriptParser<>(implementationClass, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
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

	/**
	I remember there being an annotation to specify the max time a test is allowed to run for,
	but I can't find that annotation now. so I'm implementing that logic more manually.
	*/
	@SuppressWarnings("deprecation")
	public static void runTestWithTimeLimit(long miliseconds, ThrowingRunnable<Throwable> test) {
		Thread[] threads = new Thread[2];
		StackTraceElement[][] stackTrace = new StackTraceElement[1][];
		threads[0] = new Thread(() -> {
			try {
				test.run();
			}
			catch (Throwable throwable) {
				throw AutoCodecUtil.rethrow(throwable);
			}
			finally {
				threads[1].interrupt();
			}
		});
		threads[1] = new Thread(() -> {
			try {
				Thread.sleep(miliseconds);
				stackTrace[0] = threads[0].getStackTrace();
				threads[0].stop();
			}
			catch (InterruptedException expected) {}
		});
		threads[0].start();
		threads[1].start();
		try {
			threads[0].join();
			threads[1].join();
		}
		catch (InterruptedException exception) {
			throw new RuntimeException("who interrupted the junit thread?", exception);
		}
		if (stackTrace[0] != null) {
			AssertionFailedError error = new AssertionFailedError("Infinite loop");
			error.setStackTrace(stackTrace[0]);
			throw error;
		}
	}
}