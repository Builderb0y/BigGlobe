package builderb0y.scripting.bytecode.tree.instructions.update;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class FieldUpdateInsnTreeTest {

	@Test
	public void testDeclarations() throws ScriptParsingException {
		test("class Int(int x) Int a  = Int b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) Int a  = var b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) var a  = Int b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) var a  = var b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) Int a := Int b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) Int a := var b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) var a := Int b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
		test("class Int(int x) var a := var b := Int.new(2),, a === b && a.x == 2 && b.x == 2");
	}

	@Test
	public void testIntVoid() throws ScriptParsingException {
		test("class X(int x) X x = X.new(0),, x.x  = 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, x.x += 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x -= 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x *= 2,, x.x == 4");
		test("class X(int x) X x = X.new(4),, x.x /= 2,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x %= 2,, x.x == 1");
		test("class X(int x) X x = X.new(3),, x.x ^= 2,, x.x == 9");
		test("class X(int x) X x = X.new(3),, x.x &= 6,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x |= 6,, x.x == 7");
		test("class X(int x) X x = X.new(3),, x.x #= 6,, x.x == 5");
		test("class X(int x) X x = X.new(1),, x.x <<= 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x >>= 1,, x.x == 1");
		test("class X(int x) X x = X.new(1),, x.x <<<= 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x >>>= 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, ++x.x,, x.x == 1");
		test("class X(int x) X x = X.new(1),, --x.x,, x.x == 0");
	}

	@Test
	public void testIntImplicitPreToVoid() throws ScriptParsingException {
		test("class X(int x) X x = X.new(0),, x.x =: 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, x.x +: 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x -: 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x *: 2,, x.x == 4");
		test("class X(int x) X x = X.new(4),, x.x /: 2,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x %: 2,, x.x == 1");
		test("class X(int x) X x = X.new(3),, x.x ^: 2,, x.x == 9");
		test("class X(int x) X x = X.new(3),, x.x &: 6,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x |: 6,, x.x == 7");
		test("class X(int x) X x = X.new(3),, x.x #: 6,, x.x == 5");
		test("class X(int x) X x = X.new(1),, x.x <<: 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x >>: 1,, x.x == 1");
		test("class X(int x) X x = X.new(1),, x.x <<<: 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x >>>: 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, ++:x.x,, x.x == 1");
		test("class X(int x) X x = X.new(1),, --:x.x,, x.x == 0");
	}

	@Test
	public void testIntImplicitPostToVoid() throws ScriptParsingException {
		test("class X(int x) X x = X.new(0),, x.x := 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, x.x :+ 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x :- 1,, x.x == 1");
		test("class X(int x) X x = X.new(2),, x.x :* 2,, x.x == 4");
		test("class X(int x) X x = X.new(4),, x.x :/ 2,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x :% 2,, x.x == 1");
		test("class X(int x) X x = X.new(3),, x.x :^ 2,, x.x == 9");
		test("class X(int x) X x = X.new(3),, x.x :& 6,, x.x == 2");
		test("class X(int x) X x = X.new(3),, x.x :| 6,, x.x == 7");
		test("class X(int x) X x = X.new(3),, x.x :# 6,, x.x == 5");
		test("class X(int x) X x = X.new(1),, x.x :<< 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x :>> 1,, x.x == 1");
		test("class X(int x) X x = X.new(1),, x.x :<<< 1,, x.x == 2");
		test("class X(int x) X x = X.new(2),, x.x :>>> 1,, x.x == 1");
		test("class X(int x) X x = X.new(0),, :++x.x,, x.x == 1");
		test("class X(int x) X x = X.new(1),, :--x.x,, x.x == 0");
	}

	@Test
	public void testIntPre() throws ScriptParsingException {
		test("class X(int x) X x = X.new(0),, (x.x =: 1) == 0 && x.x == 1");
		test("class X(int x) X x = X.new(0),, (x.x +: 1) == 0 && x.x == 1");
		test("class X(int x) X x = X.new(2),, (x.x -: 1) == 2 && x.x == 1");
		test("class X(int x) X x = X.new(2),, (x.x *: 2) == 2 && x.x == 4");
		test("class X(int x) X x = X.new(4),, (x.x /: 2) == 4 && x.x == 2");
		test("class X(int x) X x = X.new(3),, (x.x %: 2) == 3 && x.x == 1");
		test("class X(int x) X x = X.new(3),, (x.x ^: 2) == 3 && x.x == 9");
		test("class X(int x) X x = X.new(3),, (x.x &: 6) == 3 && x.x == 2");
		test("class X(int x) X x = X.new(3),, (x.x |: 6) == 3 && x.x == 7");
		test("class X(int x) X x = X.new(3),, (x.x #: 6) == 3 && x.x == 5");
		test("class X(int x) X x = X.new(1),, (x.x <<: 1) == 1 && x.x == 2");
		test("class X(int x) X x = X.new(2),, (x.x >>: 1) == 2 && x.x == 1");
		test("class X(int x) X x = X.new(1),, (x.x <<<: 1) == 1 && x.x == 2");
		test("class X(int x) X x = X.new(2),, (x.x >>>: 1) == 2 && x.x == 1");
		test("class X(int x) X x = X.new(0),, ++:x.x == 0 && x.x == 1");
		test("class X(int x) X x = X.new(1),, --:x.x == 1 && x.x == 0");
	}

	@Test
	public void testIntPost() throws ScriptParsingException {
		test("class X(int x) X x = X.new(0),, (x.x := 1) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(0),, (x.x :+ 1) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(2),, (x.x :- 1) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(2),, (x.x :* 2) == 4 && x.x == 4");
		test("class X(int x) X x = X.new(4),, (x.x :/ 2) == 2 && x.x == 2");
		test("class X(int x) X x = X.new(3),, (x.x :% 2) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(3),, (x.x :^ 2) == 9 && x.x == 9");
		test("class X(int x) X x = X.new(3),, (x.x :& 6) == 2 && x.x == 2");
		test("class X(int x) X x = X.new(3),, (x.x :| 6) == 7 && x.x == 7");
		test("class X(int x) X x = X.new(3),, (x.x :# 6) == 5 && x.x == 5");
		test("class X(int x) X x = X.new(1),, (x.x :<< 1) == 2 && x.x == 2");
		test("class X(int x) X x = X.new(2),, (x.x :>> 1) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(1),, (x.x :<<< 1) == 2 && x.x == 2");
		test("class X(int x) X x = X.new(2),, (x.x :>>> 1) == 1 && x.x == 1");
		test("class X(int x) X x = X.new(0),, :++x.x == 1 && x.x == 1");
		test("class X(int x) X x = X.new(1),, :--x.x == 0 && x.x == 0");
	}

	@Test
	public void testBooleanVoid() throws ScriptParsingException {
		test("class X(boolean x) X x = X.new(true ),, x.x &&= true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x &&= false,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x &&= true ,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x &&= false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ||= true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x ||= false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ||= true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ||= false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ##= true ,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ##= false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ##= true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ##= false,, x.x == false");
	}

	@Test
	public void testBooleanImplicitPreToVoid() throws ScriptParsingException {
		test("class X(boolean x) X x = X.new(true ),, x.x &&: true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x &&: false,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x &&: true ,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x &&: false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ||: true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x ||: false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ||: true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ||: false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ##: true ,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x ##: false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ##: true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x ##: false,, x.x == false");
	}

	@Test
	public void testBooleanImplicitPostToVoid() throws ScriptParsingException {
		test("class X(boolean x) X x = X.new(true ),, x.x :&& true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x :&& false,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x :&& true ,, x.x == false");
		test("class X(boolean x) X x = X.new(false),, x.x :&& false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x :|| true ,, x.x == true");
		test("class X(boolean x) X x = X.new(true ),, x.x :|| false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x :|| true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x :|| false,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x :## true ,, x.x == false");
		test("class X(boolean x) X x = X.new(true ),, x.x :## false,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x :## true ,, x.x == true");
		test("class X(boolean x) X x = X.new(false),, x.x :## false,, x.x == false");
	}

	@Test
	public void testBooleanPre() throws ScriptParsingException {
		test("class X(boolean x) X x = X.new(true ),, (x.x &&: true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(true ),, (x.x &&: false) == true  && x.x == false");
		test("class X(boolean x) X x = X.new(false),, (x.x &&: true ) == false && x.x == false");
		test("class X(boolean x) X x = X.new(false),, (x.x &&: false) == false && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x ||: true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(true ),, (x.x ||: false) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x ||: true ) == false && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x ||: false) == false && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x ##: true ) == true  && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x ##: false) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x ##: true ) == false && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x ##: false) == false && x.x == false");
	}

	@Test
	public void testBooleanPost() throws ScriptParsingException {
		test("class X(boolean x) X x = X.new(true ),, (x.x :&& true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(true ),, (x.x :&& false) == false && x.x == false");
		test("class X(boolean x) X x = X.new(false),, (x.x :&& true ) == false && x.x == false");
		test("class X(boolean x) X x = X.new(false),, (x.x :&& false) == false && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x :|| true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(true ),, (x.x :|| false) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x :|| true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x :|| false) == false && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x :## true ) == false && x.x == false");
		test("class X(boolean x) X x = X.new(true ),, (x.x :## false) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x :## true ) == true  && x.x == true");
		test("class X(boolean x) X x = X.new(false),, (x.x :## false) == false && x.x == false");
	}

	@Test
	public void testSwap() throws ScriptParsingException {
		test("class C(int x) C a = C.new(3) C b = C.new(4) a.x = b.x =: a.x a.x == 4 && b.x == 3");
	}

	public static void test(String script) throws ScriptParsingException {
		assertTrue(new ScriptParser<>(BooleanSupplier.class, script).parse());
	}
}