package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class CompareTest extends TestCommon {

	@Test
	public void testStringCompare() throws ScriptParsingException {
		assertSuccess(1, "'a' <  'b' ? 1 : 0");
		assertSuccess(0, "'a' >  'b' ? 1 : 0");
		assertSuccess(1, "'a' <= 'b' ? 1 : 0");
		assertSuccess(0, "'a' >= 'b' ? 1 : 0");
		assertSuccess(0, "'a' == 'b' ? 1 : 0");
		assertSuccess(1, "'a' != 'b' ? 1 : 0");

		assertSuccess(0, "null <  'b' ? 1 : 0");
		assertSuccess(0, "null >  'b' ? 1 : 0");
		assertSuccess(0, "null <= 'b' ? 1 : 0");
		assertSuccess(0, "null >= 'b' ? 1 : 0");
		assertSuccess(0, "null == 'b' ? 1 : 0");
		assertSuccess(1, "null != 'b' ? 1 : 0");

		assertSuccess(0, "'a' <  null ? 1 : 0");
		assertSuccess(0, "'a' >  null ? 1 : 0");
		assertSuccess(0, "'a' <= null ? 1 : 0");
		assertSuccess(0, "'a' >= null ? 1 : 0");
		assertSuccess(0, "'a' == null ? 1 : 0");
		assertSuccess(1, "'a' != null ? 1 : 0");

		assertSuccess(0, "null <  null ? 1 : 0");
		assertSuccess(0, "null >  null ? 1 : 0");
		assertSuccess(0, "null <= null ? 1 : 0");
		assertSuccess(0, "null >= null ? 1 : 0");
		assertSuccess(1, "null == null ? 1 : 0");
		assertSuccess(0, "null != null ? 1 : 0");
	}

	@Test
	public void testNumericCompare() throws ScriptParsingException {
		assertSuccess(1, "1 <  2 ? 1 : 0");
		assertSuccess(0, "1 >  2 ? 1 : 0");
		assertSuccess(1, "1 <= 2 ? 1 : 0");
		assertSuccess(0, "1 >= 2 ? 1 : 0");
		assertSuccess(0, "1 == 2 ? 1 : 0");
		assertSuccess(1, "1 != 2 ? 1 : 0");

		assertSuccess(0, "null <  2 ? 1 : 0");
		assertSuccess(0, "null >  2 ? 1 : 0");
		assertSuccess(0, "null <= 2 ? 1 : 0");
		assertSuccess(0, "null >= 2 ? 1 : 0");
		assertSuccess(0, "null == 2 ? 1 : 0");
		assertSuccess(1, "null != 2 ? 1 : 0");

		assertSuccess(0, "1 <  null ? 1 : 0");
		assertSuccess(0, "1 >  null ? 1 : 0");
		assertSuccess(0, "1 <= null ? 1 : 0");
		assertSuccess(0, "1 >= null ? 1 : 0");
		assertSuccess(0, "1 == null ? 1 : 0");
		assertSuccess(1, "1 != null ? 1 : 0");

		assertSuccess(0, "null <  null ? 1 : 0");
		assertSuccess(0, "null >  null ? 1 : 0");
		assertSuccess(0, "null <= null ? 1 : 0");
		assertSuccess(0, "null >= null ? 1 : 0");
		assertSuccess(1, "null == null ? 1 : 0");
		assertSuccess(0, "null != null ? 1 : 0");
	}
}