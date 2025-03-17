package builderb0y.scripting.parsing.special;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class PrefixedNamedValuesTest {

	@Test
	public void test() throws ScriptParsingException{
		PrefixedNamedValues values = parse("()");
		assertNull(values.prefix());
		assertEquals(0, values.values().length);

		values = parse("(1)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(0, values.values().length);

		values = parse("(x)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(0, values.values().length);

		values = parse("(a: 1)");
		assertNull(values.prefix());
		assertEquals(1, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());

		values = parse("(1, a: 1)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(1, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());

		values = parse("(x, a: 1)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(1, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());

		values = parse("(a: 1, b: 2)");
		assertNull(values.prefix());
		assertEquals(2, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());
		assertEquals("b", values.values()[1].name());
		assertEquals(2, values.values()[1].value().getConstantValue().asInt());

		values = parse("(1, a: 1, b: 2)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(2, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());
		assertEquals("b", values.values()[1].name());
		assertEquals(2, values.values()[1].value().getConstantValue().asInt());

		values = parse("(x, a: 1, b: 2)");
		assertEquals(1, values.prefix().getConstantValue().asInt());
		assertEquals(2, values.values().length);
		assertEquals("a", values.values()[0].name());
		assertEquals(1, values.values()[0].value().getConstantValue().asInt());
		assertEquals("b", values.values()[1].name());
		assertEquals(2, values.values()[1].value().getConstantValue().asInt());
	}

	public static PrefixedNamedValues parse(String text) throws ScriptParsingException {
		return PrefixedNamedValues.parse(new ScriptParser<>(Runnable.class, text).configureEnvironment(environment -> environment.addVariableConstant("x", 1)), null, null, null);
	}
}