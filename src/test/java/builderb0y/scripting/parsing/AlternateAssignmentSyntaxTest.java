package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class AlternateAssignmentSyntaxTest extends TestCommon {

	@Test
	public void test() throws ScriptParsingException {
		assertSuccess(true, "class C(int x) C c = new() c.=x(2) c.x == 2");
		assertSuccess(true, "class C(int x) C c = new() c.=$x(2).x == 2");
		assertSuccess(true, "class C(int x) C c = new() c.=$x(2) c.x == 2");
		assertSuccess(true, "class C(int x) C c = null c.=?x(2) c == null");
		assertSuccess(true, "class C(int x) C c = null c.=?$x(2) == null");
	}
}