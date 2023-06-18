package builderb0y.scripting;

import java.util.function.Supplier;

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
}