package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class BooleanOpTest extends TestCommon {

	@Test
	public void testAnd() throws ScriptParsingException {
		assertSuccess(0, "false && false ? 1 : 0");
		assertSuccess(0, "false && true  ? 1 : 0");
		assertSuccess(0, "true  && false ? 1 : 0");
		assertSuccess(1, "true  && true  ? 1 : 0");
	}

	@Test
	public void testOr() throws ScriptParsingException {
		assertSuccess(0, "false || false ? 1 : 0");
		assertSuccess(1, "false || true  ? 1 : 0");
		assertSuccess(1, "true  || false ? 1 : 0");
		assertSuccess(1, "true  || true  ? 1 : 0");
	}

	@Test
	public void testXor() throws ScriptParsingException {
		assertSuccess(0, "false ## false ? 1 : 0");
		assertSuccess(1, "false ## true  ? 1 : 0");
		assertSuccess(1, "true  ## false ? 1 : 0");
		assertSuccess(0, "true  ## true  ? 1 : 0");
	}

	@Test
	public void testNotAnd() throws ScriptParsingException {
		assertSuccess(1, "false !&& false ? 1 : 0");
		assertSuccess(1, "false !&& true  ? 1 : 0");
		assertSuccess(1, "true  !&& false ? 1 : 0");
		assertSuccess(0, "true  !&& true  ? 1 : 0");
	}

	@Test
	public void testNotOr() throws ScriptParsingException {
		assertSuccess(1, "false !|| false ? 1 : 0");
		assertSuccess(0, "false !|| true  ? 1 : 0");
		assertSuccess(0, "true  !|| false ? 1 : 0");
		assertSuccess(0, "true  !|| true  ? 1 : 0");
	}

	@Test
	public void testNotXor() throws ScriptParsingException {
		assertSuccess(1, "false !## false ? 1 : 0");
		assertSuccess(0, "false !## true  ? 1 : 0");
		assertSuccess(0, "true  !## false ? 1 : 0");
		assertSuccess(1, "true  !## true  ? 1 : 0");
	}

	@Test
	public void testBitwiseAnd() throws ScriptParsingException {
		assertSuccess(0, "0 & 0");
		assertSuccess(0, "0 & 1");
		assertSuccess(0, "1 & 0");
		assertSuccess(1, "1 & 1");
	}

	@Test
	public void testBitwiseOr() throws ScriptParsingException {
		assertSuccess(0, "0 | 0");
		assertSuccess(1, "0 | 1");
		assertSuccess(1, "1 | 0");
		assertSuccess(1, "1 | 1");
	}

	@Test
	public void testBitwiseXor() throws ScriptParsingException {
		assertSuccess(0, "0 # 0");
		assertSuccess(1, "0 # 1");
		assertSuccess(1, "1 # 0");
		assertSuccess(0, "1 # 1");
	}
}