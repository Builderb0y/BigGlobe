package builderb0y.scripting.bytecode.tree.instructions.update;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class NullableFieldUpdateInsnTreeTest {

	@Test
	public void testIntVoid() throws ScriptParsingException {
		test("class X(int x) X x = new(0),, x.?x  = 1,, x != null && x.x == 1");
		test("class X(int x) X x = new(0),, x.?x += 1,, x != null && x.x == 1");
		test("class X(int x) X x = new(2),, x.?x -= 1,, x != null && x.x == 1");
		test("class X(int x) X x = new(2),, x.?x *= 2,, x != null && x.x == 4");
		test("class X(int x) X x = new(4),, x.?x /= 2,, x != null && x.x == 2");
		test("class X(int x) X x = new(3),, x.?x %= 2,, x != null && x.x == 1");
		test("class X(int x) X x = new(3),, x.?x ^= 2,, x != null && x.x == 9");
		test("class X(int x) X x = new(3),, x.?x &= 6,, x != null && x.x == 2");
		test("class X(int x) X x = new(3),, x.?x |= 6,, x != null && x.x == 7");
		test("class X(int x) X x = new(3),, x.?x #= 6,, x != null && x.x == 5");
		test("class X(int x) X x = new(1),, x.?x <<= 1,, x != null && x.x == 2");
		test("class X(int x) X x = new(2),, x.?x >>= 1,, x != null && x.x == 1");
		test("class X(int x) X x = new(1),, x.?x <<<= 1,, x != null && x.x == 2");
		test("class X(int x) X x = new(2),, x.?x >>>= 1,, x != null && x.x == 1");
		test("class X(int x) X x = new(0),, ++x.?x,, x != null && x.x == 1");
		test("class X(int x) X x = new(1),, --x.?x,, x != null && x.x == 0");

		test("class X(int x) X x = null,, x.?x  = 1,, x == null");
		test("class X(int x) X x = null,, x.?x += 1,, x == null");
		test("class X(int x) X x = null,, x.?x -= 1,, x == null");
		test("class X(int x) X x = null,, x.?x *= 2,, x == null");
		test("class X(int x) X x = null,, x.?x /= 2,, x == null");
		test("class X(int x) X x = null,, x.?x %= 2,, x == null");
		test("class X(int x) X x = null,, x.?x ^= 2,, x == null");
		test("class X(int x) X x = null,, x.?x &= 6,, x == null");
		test("class X(int x) X x = null,, x.?x |= 6,, x == null");
		test("class X(int x) X x = null,, x.?x #= 6,, x == null");
		test("class X(int x) X x = null,, x.?x <<= 1,, x == null");
		test("class X(int x) X x = null,, x.?x >>= 1,, x == null");
		test("class X(int x) X x = null,, x.?x <<<= 1,, x == null");
		test("class X(int x) X x = null,, x.?x >>>= 1,, x == null");
		test("class X(int x) X x = null,, ++x.?x,, x == null");
		test("class X(int x) X x = null,, --x.?x,, x == null");

		test("class X(int x) int s = 0,, X x = null,, x.?x  = (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x += (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x -= (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x *= (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x /= (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x %= (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x ^= (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x &= (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x |= (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x #= (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x <<= (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x >>= (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x <<<= (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x >>>= (s = 1,, 1),, s == 0");
	}

	@Test
	public void testIntImplicitPreToVoid() throws ScriptParsingException {
		test("class X(int x) X x = new(0),, x.?x =: 1,, x.x == 1");
		test("class X(int x) X x = new(0),, x.?x +: 1,, x.x == 1");
		test("class X(int x) X x = new(2),, x.?x -: 1,, x.x == 1");
		test("class X(int x) X x = new(2),, x.?x *: 2,, x.x == 4");
		test("class X(int x) X x = new(4),, x.?x /: 2,, x.x == 2");
		test("class X(int x) X x = new(3),, x.?x %: 2,, x.x == 1");
		test("class X(int x) X x = new(3),, x.?x ^: 2,, x.x == 9");
		test("class X(int x) X x = new(3),, x.?x &: 6,, x.x == 2");
		test("class X(int x) X x = new(3),, x.?x |: 6,, x.x == 7");
		test("class X(int x) X x = new(3),, x.?x #: 6,, x.x == 5");
		test("class X(int x) X x = new(1),, x.?x <<: 1,, x.x == 2");
		test("class X(int x) X x = new(2),, x.?x >>: 1,, x.x == 1");
		test("class X(int x) X x = new(1),, x.?x <<<: 1,, x.x == 2");
		test("class X(int x) X x = new(2),, x.?x >>>: 1,, x.x == 1");
		test("class X(int x) X x = new(0),, ++:x.?x,, x.x == 1");
		test("class X(int x) X x = new(1),, --:x.?x,, x.x == 0");

		test("class X(int x) X x = null,, x.?x =: 1,, x == null");
		test("class X(int x) X x = null,, x.?x +: 1,, x == null");
		test("class X(int x) X x = null,, x.?x -: 1,, x == null");
		test("class X(int x) X x = null,, x.?x *: 2,, x == null");
		test("class X(int x) X x = null,, x.?x /: 2,, x == null");
		test("class X(int x) X x = null,, x.?x %: 2,, x == null");
		test("class X(int x) X x = null,, x.?x ^: 2,, x == null");
		test("class X(int x) X x = null,, x.?x &: 6,, x == null");
		test("class X(int x) X x = null,, x.?x |: 6,, x == null");
		test("class X(int x) X x = null,, x.?x #: 6,, x == null");
		test("class X(int x) X x = null,, x.?x <<: 1,, x == null");
		test("class X(int x) X x = null,, x.?x >>: 1,, x == null");
		test("class X(int x) X x = null,, x.?x <<<: 1,, x == null");
		test("class X(int x) X x = null,, x.?x >>>: 1,, x == null");
		test("class X(int x) X x = null,, ++:x.?x,, x == null");
		test("class X(int x) X x = null,, --:x.?x,, x == null");

		test("class X(int x) int s = 0,, X x = null,, x.?x =: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x +: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x -: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x *: (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x /: (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x %: (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x ^: (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x &: (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x |: (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x #: (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x <<: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x >>: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x <<<: (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x >>>: (s = 1,, 1),, s == 0");
	}

	@Test
	public void testIntImplicitPostToVoid() throws ScriptParsingException {
		test("class X(int x) X x = new(0),, x.?x := 1,, x.x == 1");
		test("class X(int x) X x = new(0),, x.?x :+ 1,, x.x == 1");
		test("class X(int x) X x = new(2),, x.?x :- 1,, x.x == 1");
		test("class X(int x) X x = new(2),, x.?x :* 2,, x.x == 4");
		test("class X(int x) X x = new(4),, x.?x :/ 2,, x.x == 2");
		test("class X(int x) X x = new(3),, x.?x :% 2,, x.x == 1");
		test("class X(int x) X x = new(3),, x.?x :^ 2,, x.x == 9");
		test("class X(int x) X x = new(3),, x.?x :& 6,, x.x == 2");
		test("class X(int x) X x = new(3),, x.?x :| 6,, x.x == 7");
		test("class X(int x) X x = new(3),, x.?x :# 6,, x.x == 5");
		test("class X(int x) X x = new(1),, x.?x :<< 1,, x.x == 2");
		test("class X(int x) X x = new(2),, x.?x :>> 1,, x.x == 1");
		test("class X(int x) X x = new(1),, x.?x :<<< 1,, x.x == 2");
		test("class X(int x) X x = new(2),, x.?x :>>> 1,, x.x == 1");
		test("class X(int x) X x = new(0),, :++x.?x,, x.x == 1");
		test("class X(int x) X x = new(1),, :--x.?x,, x.x == 0");

		test("class X(int x) X x = null,, x.?x := 1,, x == null");
		test("class X(int x) X x = null,, x.?x :+ 1,, x == null");
		test("class X(int x) X x = null,, x.?x :- 1,, x == null");
		test("class X(int x) X x = null,, x.?x :* 2,, x == null");
		test("class X(int x) X x = null,, x.?x :/ 2,, x == null");
		test("class X(int x) X x = null,, x.?x :% 2,, x == null");
		test("class X(int x) X x = null,, x.?x :^ 2,, x == null");
		test("class X(int x) X x = null,, x.?x :& 6,, x == null");
		test("class X(int x) X x = null,, x.?x :| 6,, x == null");
		test("class X(int x) X x = null,, x.?x :# 6,, x == null");
		test("class X(int x) X x = null,, x.?x :<< 1,, x == null");
		test("class X(int x) X x = null,, x.?x :>> 1,, x == null");
		test("class X(int x) X x = null,, x.?x :<<< 1,, x == null");
		test("class X(int x) X x = null,, x.?x :>>> 1,, x == null");
		test("class X(int x) X x = null,, :++x.?x,, x == null");
		test("class X(int x) X x = null,, :--x.?x,, x == null");

		test("class X(int x) int s = 0,, X x = null,, x.?x := (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :+ (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :- (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :* (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :/ (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :% (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :^ (s = 1,, 2),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :& (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :| (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :# (s = 1,, 6),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :<< (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :>> (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :<<< (s = 1,, 1),, s == 0");
		test("class X(int x) int s = 0,, X x = null,, x.?x :>>> (s = 1,, 1),, s == 0");
	}

	@Test
	public void testIntPre() throws ScriptParsingException {
		test("class X(int x) X x = new(0),, (x.?x =: 1) == 0 && x.x == 1");
		test("class X(int x) X x = new(0),, (x.?x +: 1) == 0 && x.x == 1");
		test("class X(int x) X x = new(2),, (x.?x -: 1) == 2 && x.x == 1");
		test("class X(int x) X x = new(2),, (x.?x *: 2) == 2 && x.x == 4");
		test("class X(int x) X x = new(4),, (x.?x /: 2) == 4 && x.x == 2");
		test("class X(int x) X x = new(3),, (x.?x %: 2) == 3 && x.x == 1");
		test("class X(int x) X x = new(3),, (x.?x ^: 2) == 3 && x.x == 9");
		test("class X(int x) X x = new(3),, (x.?x &: 6) == 3 && x.x == 2");
		test("class X(int x) X x = new(3),, (x.?x |: 6) == 3 && x.x == 7");
		test("class X(int x) X x = new(3),, (x.?x #: 6) == 3 && x.x == 5");
		test("class X(int x) X x = new(1),, (x.?x <<: 1) == 1 && x.x == 2");
		test("class X(int x) X x = new(2),, (x.?x >>: 1) == 2 && x.x == 1");
		test("class X(int x) X x = new(1),, (x.?x <<<: 1) == 1 && x.x == 2");
		test("class X(int x) X x = new(2),, (x.?x >>>: 1) == 2 && x.x == 1");
		test("class X(int x) X x = new(0),, ++:x.?x == 0 && x.x == 1");
		test("class X(int x) X x = new(1),, --:x.?x == 1 && x.x == 0");

		test("class X(int x) X x = null,, (x.?x =: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x +: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x -: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x *: 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x /: 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x %: 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x ^: 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x &: 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x |: 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x #: 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x <<: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x >>: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x <<<: 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x >>>: 1) == 0 && x == null");
		test("class X(int x) X x = null,, ++:x.?x == 0 && x == null");
		test("class X(int x) X x = null,, --:x.?x == 0 && x == null");

		test("class X(int x) int s = 0,, X x = null,, (x.?x =: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x +: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x -: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x *: (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x /: (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x %: (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x ^: (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x &: (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x |: (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x #: (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x <<: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x >>: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x <<<: (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x >>>: (s = 1,, 1)) == 0 && x == null && s == 0");
	}

	@Test
	public void testIntPost() throws ScriptParsingException {
		test("class X(int x) X x = new(0),, (x.?x := 1) == 1 && x.x == 1");
		test("class X(int x) X x = new(0),, (x.?x :+ 1) == 1 && x.x == 1");
		test("class X(int x) X x = new(2),, (x.?x :- 1) == 1 && x.x == 1");
		test("class X(int x) X x = new(2),, (x.?x :* 2) == 4 && x.x == 4");
		test("class X(int x) X x = new(4),, (x.?x :/ 2) == 2 && x.x == 2");
		test("class X(int x) X x = new(3),, (x.?x :% 2) == 1 && x.x == 1");
		test("class X(int x) X x = new(3),, (x.?x :^ 2) == 9 && x.x == 9");
		test("class X(int x) X x = new(3),, (x.?x :& 6) == 2 && x.x == 2");
		test("class X(int x) X x = new(3),, (x.?x :| 6) == 7 && x.x == 7");
		test("class X(int x) X x = new(3),, (x.?x :# 6) == 5 && x.x == 5");
		test("class X(int x) X x = new(1),, (x.?x :<< 1) == 2 && x.x == 2");
		test("class X(int x) X x = new(2),, (x.?x :>> 1) == 1 && x.x == 1");
		test("class X(int x) X x = new(1),, (x.?x :<<< 1) == 2 && x.x == 2");
		test("class X(int x) X x = new(2),, (x.?x :>>> 1) == 1 && x.x == 1");
		test("class X(int x) X x = new(0),, :++x.?x == 1 && x.x == 1");
		test("class X(int x) X x = new(1),, :--x.?x == 0 && x.x == 0");

		test("class X(int x) X x = null,, (x.?x := 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :+ 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :- 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :* 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :/ 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :% 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :^ 2) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :& 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :| 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :# 6) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :<< 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :>> 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :<<< 1) == 0 && x == null");
		test("class X(int x) X x = null,, (x.?x :>>> 1) == 0 && x == null");
		test("class X(int x) X x = null,, :++x.?x == 0 && x == null");
		test("class X(int x) X x = null,, :--x.?x == 0 && x == null");

		test("class X(int x) int s = 0,, X x = null,, (x.?x := (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :+ (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :- (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :* (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :/ (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :% (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :^ (s = 1,, 2)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :& (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :| (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :# (s = 1,, 6)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :<< (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :>> (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :<<< (s = 1,, 1)) == 0 && x == null && s == 0");
		test("class X(int x) int s = 0,, X x = null,, (x.?x :>>> (s = 1,, 1)) == 0 && x == null && s == 0");
	}

	public static void test(String script) throws ScriptParsingException {
		assertTrue(new ScriptParser<>(BooleanSupplier.class, script).parse());
	}
}