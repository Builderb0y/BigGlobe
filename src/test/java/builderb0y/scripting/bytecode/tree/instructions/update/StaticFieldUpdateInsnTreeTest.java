package builderb0y.scripting.bytecode.tree.instructions.update;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.ScriptInterfaces.BooleanSupplier;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class StaticFieldUpdateInsnTreeTest {

	public static int x, y;
	public static boolean b;

	@Test
	public void testIntVoid() throws ScriptParsingException {
		test("x = 0,, x  = 1,, x == 1");
		test("x = 0,, x += 1,, x == 1");
		test("x = 2,, x -= 1,, x == 1");
		test("x = 2,, x *= 2,, x == 4");
		test("x = 4,, x /= 2,, x == 2");
		test("x = 3,, x %= 2,, x == 1");
		test("x = 3,, x ^= 2,, x == 9");
		test("x = 3,, x &= 6,, x == 2");
		test("x = 3,, x |= 6,, x == 7");
		test("x = 3,, x #= 6,, x == 5");
		test("x = 1,, x <<= 1,, x == 2");
		test("x = 2,, x >>= 1,, x == 1");
		test("x = 1,, x <<<= 1,, x == 2");
		test("x = 2,, x >>>= 1,, x == 1");
		test("x = 0,, ++x,, x == 1");
		test("x = 1,, --x,, x == 0");
	}

	@Test
	public void testIntImplicitPreToVoid() throws ScriptParsingException {
		test("x = 0,, x =: 1,, x == 1");
		test("x = 0,, x +: 1,, x == 1");
		test("x = 2,, x -: 1,, x == 1");
		test("x = 2,, x *: 2,, x == 4");
		test("x = 4,, x /: 2,, x == 2");
		test("x = 3,, x %: 2,, x == 1");
		test("x = 3,, x ^: 2,, x == 9");
		test("x = 3,, x &: 6,, x == 2");
		test("x = 3,, x |: 6,, x == 7");
		test("x = 3,, x #: 6,, x == 5");
		test("x = 1,, x <<: 1,, x == 2");
		test("x = 2,, x >>: 1,, x == 1");
		test("x = 1,, x <<<: 1,, x == 2");
		test("x = 2,, x >>>: 1,, x == 1");
		test("x = 0,, ++:x,, x == 1");
		test("x = 1,, --:x,, x == 0");
	}

	@Test
	public void testIntImplicitPostToVoid() throws ScriptParsingException {
		test("x = 0,, x := 1,, x == 1");
		test("x = 0,, x :+ 1,, x == 1");
		test("x = 2,, x :- 1,, x == 1");
		test("x = 2,, x :* 2,, x == 4");
		test("x = 4,, x :/ 2,, x == 2");
		test("x = 3,, x :% 2,, x == 1");
		test("x = 3,, x :^ 2,, x == 9");
		test("x = 3,, x :& 6,, x == 2");
		test("x = 3,, x :| 6,, x == 7");
		test("x = 3,, x :# 6,, x == 5");
		test("x = 1,, x :<< 1,, x == 2");
		test("x = 2,, x :>> 1,, x == 1");
		test("x = 1,, x :<<< 1,, x == 2");
		test("x = 2,, x :>>> 1,, x == 1");
		test("x = 0,, :++x,, x == 1");
		test("x = 1,, :--x,, x == 0");
	}

	@Test
	public void testIntPre() throws ScriptParsingException {
		test("x = 0,, (x =: 1) == 0 && x == 1");
		test("x = 0,, (x +: 1) == 0 && x == 1");
		test("x = 2,, (x -: 1) == 2 && x == 1");
		test("x = 2,, (x *: 2) == 2 && x == 4");
		test("x = 4,, (x /: 2) == 4 && x == 2");
		test("x = 3,, (x %: 2) == 3 && x == 1");
		test("x = 3,, (x ^: 2) == 3 && x == 9");
		test("x = 3,, (x &: 6) == 3 && x == 2");
		test("x = 3,, (x |: 6) == 3 && x == 7");
		test("x = 3,, (x #: 6) == 3 && x == 5");
		test("x = 1,, (x <<: 1) == 1 && x == 2");
		test("x = 2,, (x >>: 1) == 2 && x == 1");
		test("x = 1,, (x <<<: 1) == 1 && x == 2");
		test("x = 2,, (x >>>: 1) == 2 && x == 1");
		test("x = 0,, ++:x == 0 && x == 1");
		test("x = 1,, --:x == 1 && x == 0");
	}

	@Test
	public void testIntPost() throws ScriptParsingException {
		test("x = 0,, (x := 1) == 1 && x == 1");
		test("x = 0,, (x :+ 1) == 1 && x == 1");
		test("x = 2,, (x :- 1) == 1 && x == 1");
		test("x = 2,, (x :* 2) == 4 && x == 4");
		test("x = 4,, (x :/ 2) == 2 && x == 2");
		test("x = 3,, (x :% 2) == 1 && x == 1");
		test("x = 3,, (x :^ 2) == 9 && x == 9");
		test("x = 3,, (x :& 6) == 2 && x == 2");
		test("x = 3,, (x :| 6) == 7 && x == 7");
		test("x = 3,, (x :# 6) == 5 && x == 5");
		test("x = 1,, (x :<< 1) == 2 && x == 2");
		test("x = 2,, (x :>> 1) == 1 && x == 1");
		test("x = 1,, (x :<<< 1) == 2 && x == 2");
		test("x = 2,, (x :>>> 1) == 1 && x == 1");
		test("x = 0,, :++x == 1 && x == 1");
		test("x = 1,, :--x == 0 && x == 0");
	}

	@Test
	public void testBooleanVoid() throws ScriptParsingException {
		test("b = true ,, b &&= true ,, b == true");
		test("b = true ,, b &&= false,, b == false");
		test("b = false,, b &&= true ,, b == false");
		test("b = false,, b &&= false,, b == false");
		test("b = true ,, b ||= true ,, b == true");
		test("b = true ,, b ||= false,, b == true");
		test("b = false,, b ||= true ,, b == true");
		test("b = false,, b ||= false,, b == false");
		test("b = true ,, b ##= true ,, b == false");
		test("b = true ,, b ##= false,, b == true");
		test("b = false,, b ##= true ,, b == true");
		test("b = false,, b ##= false,, b == false");
	}

	@Test
	public void testBooleanImplicitPreToVoid() throws ScriptParsingException {
		test("b = true ,, b &&: true ,, b == true");
		test("b = true ,, b &&: false,, b == false");
		test("b = false,, b &&: true ,, b == false");
		test("b = false,, b &&: false,, b == false");
		test("b = true ,, b ||: true ,, b == true");
		test("b = true ,, b ||: false,, b == true");
		test("b = false,, b ||: true ,, b == true");
		test("b = false,, b ||: false,, b == false");
		test("b = true ,, b ##: true ,, b == false");
		test("b = true ,, b ##: false,, b == true");
		test("b = false,, b ##: true ,, b == true");
		test("b = false,, b ##: false,, b == false");
	}

	@Test
	public void testBooleanImplicitPostToVoid() throws ScriptParsingException {
		test("b = true ,, b :&& true ,, b == true");
		test("b = true ,, b :&& false,, b == false");
		test("b = false,, b :&& true ,, b == false");
		test("b = false,, b :&& false,, b == false");
		test("b = true ,, b :|| true ,, b == true");
		test("b = true ,, b :|| false,, b == true");
		test("b = false,, b :|| true ,, b == true");
		test("b = false,, b :|| false,, b == false");
		test("b = true ,, b :## true ,, b == false");
		test("b = true ,, b :## false,, b == true");
		test("b = false,, b :## true ,, b == true");
		test("b = false,, b :## false,, b == false");
	}

	@Test
	public void testBooleanPre() throws ScriptParsingException {
		test("b = true ,, (b &&: true ) == true  && b == true");
		test("b = true ,, (b &&: false) == true  && b == false");
		test("b = false,, (b &&: true ) == false && b == false");
		test("b = false,, (b &&: false) == false && b == false");
		test("b = true ,, (b ||: true ) == true  && b == true");
		test("b = true ,, (b ||: false) == true  && b == true");
		test("b = false,, (b ||: true ) == false && b == true");
		test("b = false,, (b ||: false) == false && b == false");
		test("b = true ,, (b ##: true ) == true  && b == false");
		test("b = true ,, (b ##: false) == true  && b == true");
		test("b = false,, (b ##: true ) == false && b == true");
		test("b = false,, (b ##: false) == false && b == false");
	}

	@Test
	public void testBooleanPost() throws ScriptParsingException {
		test("b = true ,, (b :&& true ) == true  && b == true");
		test("b = true ,, (b :&& false) == false && b == false");
		test("b = false,, (b :&& true ) == false && b == false");
		test("b = false,, (b :&& false) == false && b == false");
		test("b = true ,, (b :|| true ) == true  && b == true");
		test("b = true ,, (b :|| false) == true  && b == true");
		test("b = false,, (b :|| true ) == true  && b == true");
		test("b = false,, (b :|| false) == false && b == false");
		test("b = true ,, (b :## true ) == false && b == false");
		test("b = true ,, (b :## false) == true  && b == true");
		test("b = false,, (b :## true ) == true  && b == true");
		test("b = false,, (b :## false) == false && b == false");
	}

	@Test
	public void testSwap() throws ScriptParsingException {
		test("x = 3 y = 4 x = y =: x x == 4 && y == 3");
	}

	public static void test(String script) throws ScriptParsingException {
		assertTrue(
			new ScriptParser<>(BooleanSupplier.class, script)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableGetStatics(StaticFieldUpdateInsnTreeTest.class, "x", "y", "b")
			)
			.parse()
			.getAsBoolean()
		);
	}
}