package builderb0y.scripting;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.util.Printer;
import org.opentest4j.AssertionFailedError;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.classes.spec.BuiltinType;
import builderb0y.bigglobe.util.ThrowingRunnable;
import builderb0y.scripting.ScriptInterfaces.ObjectSupplier;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
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
		BuiltinType.JUnit.TESTING = true;
		return (
			new ScriptParser<>(ObjectSupplier.class, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				for (BuiltinType type : BuiltinType.UNIVERSAL) {
					type.setupEnvironment(environment);
				}
				environment.addMethodInvoke(Object.class, "getClass");
			})
			.parse(new ScriptClassLoader())
			.getAsObject()
		);
	}

	public static void assertOpcodes(String input, Class<?> implementationClass, int... expectedOpcodes) throws ScriptParsingException {
		ScriptParser<?> parser = (
			new ScriptParser<>(implementationClass, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
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
	public static void runTestWithTimeLimit(long milliseconds, ThrowingRunnable<Throwable> test) {
		Throwable[] stackTrace = new Throwable[1];
		Thread thread = new Thread(() -> {
			try {
				test.run();
			}
			catch (Throwable throwable) {
				stackTrace[0] = throwable;
			}
		});
		thread.start();
		try {
			thread.join(milliseconds);
		}
		catch (InterruptedException exception) {
			exception.printStackTrace();
		}
		finally {
			if (thread.isAlive()) {
				AssertionFailedError error = new AssertionFailedError("Infinite loop");
				error.setStackTrace(thread.getStackTrace());
				error.addSuppressed(new AssertionFailedError("Calling thread stack trace:"));
				error.printStackTrace();
				System.err.println("Since java removed Thread.stop(), I have no choice but to halt the entire JVM instead. Consider this test failed. See the above stack trace for more info.");
				Runtime.getRuntime().halt(1);
			}
		}
	}
}