package builderb0y.scripting.bytecode.tree.instructions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.ClassOptimizerTest.dumpBytecode;

public class ScaleByPowerOf2ToShiftTest {

	@Test
	@Disabled
	public void testInt() throws ScriptParsingException {
		dumpBytecode("int x = 42,, return(1024 * x / 1024 % 64)");
	}

	@Test
	@Disabled
	public void testLong() throws ScriptParsingException {
		dumpBytecode("long x = 42L,, return(1024L * x / 1024L % 64L)");
	}
}