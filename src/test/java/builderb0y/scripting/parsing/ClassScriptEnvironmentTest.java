package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.environments.ClassScriptEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class ClassScriptEnvironmentTest {

	@Test
	public void testArrayList() throws ScriptParsingException {
		assertSuccess(2,
			"""
			List list = ArrayList.new(3)
			list.add(1)
			list.add(2)
			list.add(3)
			list.get(1)
			"""
		);
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

	public static Object evaluate(String script) throws ScriptParsingException {
		return (
			new ScriptParser<>(Supplier.class, script)
			.addEnvironment(new ClassScriptEnvironment(ArrayList.class))
			.addEnvironment(new ClassScriptEnvironment(List.class))
			.addEnvironment(new ClassScriptEnvironment(Collection.class))
			.addEnvironment(new ClassScriptEnvironment(Iterable.class))
			.parse()
			.get()
		);
	}
}