package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class SpecialMemberTypesTest extends TestCommon {

	@Test
	public void testNullability() throws ScriptParsingException {
		assertSuccess(2, "class C(int x) C c = new(2) c.x");
		assertSuccess(2, "class C(int x) C c = new(2) c.?x");
		assertSuccess(null, "class C(Integer x) C c = null c.?x");
		assertSuccess(2, "class C(int x) C c = null Integer(int(c.?x ?: 2))");
		assertSuccess(2, "class C(int x) C c = null Integer(int(c.?x() ?: 2))");
	}

	@Test
	public void testReceiver() throws ScriptParsingException {
		assertSuccess(true, "class C(int x) C c = new() C c2 = c.$x(2) c === c2");
		assertSuccess(2, "class C(int x) C c = new() c.$x(2).x");
		assertSuccess(2, "class C(int x) C c = new() c.$x(2).x()");
	}

	@Test
	public void testNullableReceiver() throws ScriptParsingException {
		assertSuccess(true,
			"""
			class C(int x)
			C c = null
			C c2 = c.$?x(2)
			c2 == null
			"""
		);
		assertSuccess(true,
			"""
			class C(int x)
			C c = null
			C c2 = c.$?x(2)
			c2 == null
			"""
		);
		assertSuccess(0,
			"""
			class C(int x)
			int sideEffect = 0
			C c = null
			C c2 = c.$?x(sideEffect := 2)
			sideEffect
			"""
		);
	}
}