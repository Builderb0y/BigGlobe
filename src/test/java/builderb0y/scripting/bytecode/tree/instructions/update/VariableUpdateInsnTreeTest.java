package builderb0y.scripting.bytecode.tree.instructions.update;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class VariableUpdateInsnTreeTest {

	@Test
	public void testDeclarations() throws ScriptParsingException {
		test("int a  = int b := 2,, a == 2 && b == 2");
		test("int a  = var b := 2,, a == 2 && b == 2");
		test("var a  = int b := 2,, a == 2 && b == 2");
		test("var a  = var b := 2,, a == 2 && b == 2");
		test("int a := int b := 2,, a == 2 && b == 2");
		test("int a := var b := 2,, a == 2 && b == 2");
		test("var a := int b := 2,, a == 2 && b == 2");
		test("var a := var b := 2,, a == 2 && b == 2");
	}

	@Test
	public void testIntVoid() throws ScriptParsingException {
		test("int x = 0,, x  = 1,, x == 1");
		test("int x = 0,, x += 1,, x == 1");
		test("int x = 2,, x -= 1,, x == 1");
		test("int x = 2,, x *= 2,, x == 4");
		test("int x = 4,, x /= 2,, x == 2");
		test("int x = 3,, x %= 2,, x == 1");
		test("int x = 3,, x ^= 2,, x == 9");
		test("int x = 3,, x &= 6,, x == 2");
		test("int x = 3,, x |= 6,, x == 7");
		test("int x = 3,, x #= 6,, x == 5");
		test("int x = 1,, x <<= 1,, x == 2");
		test("int x = 2,, x >>= 1,, x == 1");
		test("int x = 1,, x <<<= 1,, x == 2");
		test("int x = 2,, x >>>= 1,, x == 1");
		test("int x = 0,, ++x,, x == 1");
		test("int x = 1,, --x,, x == 0");
	}

	@Test
	public void testIntImplicitPreToVoid() throws ScriptParsingException {
		test("int x = 0,, x =: 1,, x == 1");
		test("int x = 0,, x +: 1,, x == 1");
		test("int x = 2,, x -: 1,, x == 1");
		test("int x = 2,, x *: 2,, x == 4");
		test("int x = 4,, x /: 2,, x == 2");
		test("int x = 3,, x %: 2,, x == 1");
		test("int x = 3,, x ^: 2,, x == 9");
		test("int x = 3,, x &: 6,, x == 2");
		test("int x = 3,, x |: 6,, x == 7");
		test("int x = 3,, x #: 6,, x == 5");
		test("int x = 1,, x <<: 1,, x == 2");
		test("int x = 2,, x >>: 1,, x == 1");
		test("int x = 1,, x <<<: 1,, x == 2");
		test("int x = 2,, x >>>: 1,, x == 1");
		test("int x = 0,, ++:x,, x == 1");
		test("int x = 1,, --:x,, x == 0");
	}

	@Test
	public void testIntImplicitPostToVoid() throws ScriptParsingException {
		test("int x = 0,, x := 1,, x == 1");
		test("int x = 0,, x :+ 1,, x == 1");
		test("int x = 2,, x :- 1,, x == 1");
		test("int x = 2,, x :* 2,, x == 4");
		test("int x = 4,, x :/ 2,, x == 2");
		test("int x = 3,, x :% 2,, x == 1");
		test("int x = 3,, x :^ 2,, x == 9");
		test("int x = 3,, x :& 6,, x == 2");
		test("int x = 3,, x :| 6,, x == 7");
		test("int x = 3,, x :# 6,, x == 5");
		test("int x = 1,, x :<< 1,, x == 2");
		test("int x = 2,, x :>> 1,, x == 1");
		test("int x = 1,, x :<<< 1,, x == 2");
		test("int x = 2,, x :>>> 1,, x == 1");
		test("int x = 0,, :++x,, x == 1");
		test("int x = 1,, :--x,, x == 0");
	}

	@Test
	public void testIntPre() throws ScriptParsingException {
		test("int x = 0,, (x =: 1) == 0 && x == 1");
		test("int x = 0,, (x +: 1) == 0 && x == 1");
		test("int x = 2,, (x -: 1) == 2 && x == 1");
		test("int x = 2,, (x *: 2) == 2 && x == 4");
		test("int x = 4,, (x /: 2) == 4 && x == 2");
		test("int x = 3,, (x %: 2) == 3 && x == 1");
		test("int x = 3,, (x ^: 2) == 3 && x == 9");
		test("int x = 3,, (x &: 6) == 3 && x == 2");
		test("int x = 3,, (x |: 6) == 3 && x == 7");
		test("int x = 3,, (x #: 6) == 3 && x == 5");
		test("int x = 1,, (x <<: 1) == 1 && x == 2");
		test("int x = 2,, (x >>: 1) == 2 && x == 1");
		test("int x = 1,, (x <<<: 1) == 1 && x == 2");
		test("int x = 2,, (x >>>: 1) == 2 && x == 1");
		test("int x = 0,, ++:x == 0 && x == 1");
		test("int x = 1,, --:x == 1 && x == 0");
	}

	@Test
	public void testIntPost() throws ScriptParsingException {
		test("int x = 0,, (x := 1) == 1 && x == 1");
		test("int x = 0,, (x :+ 1) == 1 && x == 1");
		test("int x = 2,, (x :- 1) == 1 && x == 1");
		test("int x = 2,, (x :* 2) == 4 && x == 4");
		test("int x = 4,, (x :/ 2) == 2 && x == 2");
		test("int x = 3,, (x :% 2) == 1 && x == 1");
		test("int x = 3,, (x :^ 2) == 9 && x == 9");
		test("int x = 3,, (x :& 6) == 2 && x == 2");
		test("int x = 3,, (x :| 6) == 7 && x == 7");
		test("int x = 3,, (x :# 6) == 5 && x == 5");
		test("int x = 1,, (x :<< 1) == 2 && x == 2");
		test("int x = 2,, (x :>> 1) == 1 && x == 1");
		test("int x = 1,, (x :<<< 1) == 2 && x == 2");
		test("int x = 2,, (x :>>> 1) == 1 && x == 1");
		test("int x = 0,, :++x == 1 && x == 1");
		test("int x = 1,, :--x == 0 && x == 0");
	}

	@Test
	public void testBooleanVoid() throws ScriptParsingException {
		test("boolean x = true ,, x &&= true ,, x == true");
		test("boolean x = true ,, x &&= false,, x == false");
		test("boolean x = false,, x &&= true ,, x == false");
		test("boolean x = false,, x &&= false,, x == false");
		test("boolean x = true ,, x ||= true ,, x == true");
		test("boolean x = true ,, x ||= false,, x == true");
		test("boolean x = false,, x ||= true ,, x == true");
		test("boolean x = false,, x ||= false,, x == false");
		test("boolean x = true ,, x ##= true ,, x == false");
		test("boolean x = true ,, x ##= false,, x == true");
		test("boolean x = false,, x ##= true ,, x == true");
		test("boolean x = false,, x ##= false,, x == false");
	}

	@Test
	public void testBooleanImplicitPreToVoid() throws ScriptParsingException {
		test("boolean x = true ,, x &&: true ,, x == true");
		test("boolean x = true ,, x &&: false,, x == false");
		test("boolean x = false,, x &&: true ,, x == false");
		test("boolean x = false,, x &&: false,, x == false");
		test("boolean x = true ,, x ||: true ,, x == true");
		test("boolean x = true ,, x ||: false,, x == true");
		test("boolean x = false,, x ||: true ,, x == true");
		test("boolean x = false,, x ||: false,, x == false");
		test("boolean x = true ,, x ##: true ,, x == false");
		test("boolean x = true ,, x ##: false,, x == true");
		test("boolean x = false,, x ##: true ,, x == true");
		test("boolean x = false,, x ##: false,, x == false");
	}

	@Test
	public void testBooleanImplicitPostToVoid() throws ScriptParsingException {
		test("boolean x = true ,, x :&& true ,, x == true");
		test("boolean x = true ,, x :&& false,, x == false");
		test("boolean x = false,, x :&& true ,, x == false");
		test("boolean x = false,, x :&& false,, x == false");
		test("boolean x = true ,, x :|| true ,, x == true");
		test("boolean x = true ,, x :|| false,, x == true");
		test("boolean x = false,, x :|| true ,, x == true");
		test("boolean x = false,, x :|| false,, x == false");
		test("boolean x = true ,, x :## true ,, x == false");
		test("boolean x = true ,, x :## false,, x == true");
		test("boolean x = false,, x :## true ,, x == true");
		test("boolean x = false,, x :## false,, x == false");
	}

	@Test
	public void testBooleanPre() throws ScriptParsingException {
		test("boolean x = true ,, (x &&: true ) == true  && x == true");
		test("boolean x = true ,, (x &&: false) == true  && x == false");
		test("boolean x = false,, (x &&: true ) == false && x == false");
		test("boolean x = false,, (x &&: false) == false && x == false");
		test("boolean x = true ,, (x ||: true ) == true  && x == true");
		test("boolean x = true ,, (x ||: false) == true  && x == true");
		test("boolean x = false,, (x ||: true ) == false && x == true");
		test("boolean x = false,, (x ||: false) == false && x == false");
		test("boolean x = true ,, (x ##: true ) == true  && x == false");
		test("boolean x = true ,, (x ##: false) == true  && x == true");
		test("boolean x = false,, (x ##: true ) == false && x == true");
		test("boolean x = false,, (x ##: false) == false && x == false");
	}

	@Test
	public void testBooleanPost() throws ScriptParsingException {
		test("boolean x = true ,, (x :&& true ) == true  && x == true");
		test("boolean x = true ,, (x :&& false) == false && x == false");
		test("boolean x = false,, (x :&& true ) == false && x == false");
		test("boolean x = false,, (x :&& false) == false && x == false");
		test("boolean x = true ,, (x :|| true ) == true  && x == true");
		test("boolean x = true ,, (x :|| false) == true  && x == true");
		test("boolean x = false,, (x :|| true ) == true  && x == true");
		test("boolean x = false,, (x :|| false) == false && x == false");
		test("boolean x = true ,, (x :## true ) == false && x == false");
		test("boolean x = true ,, (x :## false) == true  && x == true");
		test("boolean x = false,, (x :## true ) == true  && x == true");
		test("boolean x = false,, (x :## false) == false && x == false");
	}

	@Test
	public void testSwap() throws ScriptParsingException {
		test("int x = 3 int y = 4 x = y =: x x == 4 && y == 3");
	}

	public static void test(String script) throws ScriptParsingException {
		assertTrue(new ScriptParser<>(BooleanSupplier.class, script).parse());
	}
}