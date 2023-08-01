package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class UpdaterTest extends TestCommon {

	@Test
	public void testIntUpdaters() throws ScriptParsingException {
		assertSuccess( 1, "int x = 0 ,,   ++  x ,, x");
		assertSuccess(-1, "int x = 0 ,,   --  x ,, x");
		assertSuccess( 1, "int x = 0 ,, x +=  1 ,, x");
		assertSuccess( 0, "int x = 0 ,, x +=  0 ,, x");
		assertSuccess(-1, "int x = 0 ,, x -=  1 ,, x");
		assertSuccess( 0, "int x = 0 ,, x -=  0 ,, x");
		assertSuccess( 9, "int x = 3 ,, x *=  3 ,, x");
		assertSuccess( 6, "int x = 3 ,, x *=  2 ,, x");
		assertSuccess( 3, "int x = 3 ,, x *=  1 ,, x");
		assertSuccess( 0, "int x = 3 ,, x *=  0 ,, x");
		assertSuccess(-3, "int x = 3 ,, x *= -1 ,, x");
		assertSuccess( 2, "int x = 6 ,, x /=  3 ,, x");
		assertSuccess( 3, "int x = 6 ,, x /=  2 ,, x");
		assertSuccess( 6, "int x = 6 ,, x /=  1 ,, x");
		assertSuccess(-6, "int x = 6 ,, x /= -1 ,, x");
		assertSuccess( 0, "int x = 6 ,, x %=  3 ,, x");
		assertSuccess( 0, "int x = 6 ,, x %=  2 ,, x");
		assertSuccess( 0, "int x = 5 ,, x %=  1 ,, x");
		assertSuccess( 0, "int x = 5 ,, x %=  0 ,, x");
		assertSuccess( 4, "int x = 2 ,, x ^=  2 ,, x");
		assertSuccess( 2, "int x = 3 ,, x &=  6 ,, x");
		assertSuccess( 7, "int x = 3 ,, x |=  6 ,, x");
		assertSuccess( 5, "int x = 3 ,, x #=  6 ,, x");
	}

	@Test
	public void testDoubleUpdaters() throws ScriptParsingException {
		assertSuccess( 1, "double x = 0 ,, ++ x ,, x");
		assertSuccess(-1, "double x = 0 ,, -- x ,, x");
		assertSuccess( 1, "double x = 0 ,, x +=  1 ,, x");
		assertSuccess( 0, "double x = 0 ,, x +=  0 ,, x");
		assertSuccess(-1, "double x = 0 ,, x -=  1 ,, x");
		assertSuccess( 0, "double x = 0 ,, x -=  0 ,, x");
		assertSuccess( 9, "double x = 3 ,, x *=  3 ,, x");
		assertSuccess( 6, "double x = 3 ,, x *=  2 ,, x");
		assertSuccess( 3, "double x = 3 ,, x *=  1 ,, x");
		assertSuccess( 0, "double x = 3 ,, x *=  0 ,, x");
		assertSuccess(-3, "double x = 3 ,, x *= -1 ,, x");
		assertSuccess( 2, "double x = 6 ,, x /=  3 ,, x");
		assertSuccess( 3, "double x = 6 ,, x /=  2 ,, x");
		assertSuccess( 6, "double x = 6 ,, x /=  1 ,, x");
		assertSuccess(-6, "double x = 6 ,, x /= -1 ,, x");
		assertSuccess( 0, "double x = 6 ,, x %=  3 ,, x");
		assertSuccess( 0, "double x = 6 ,, x %=  2 ,, x");
		assertSuccess( 0, "double x = 5 ,, x %=  1 ,, x");
		assertSuccess( 0, "double x = 5 ,, x %=  0 ,, x");
		assertSuccess( 4, "double x = 2 ,, x ^=  2 ,, x");
	}

	@Test
	public void testBooleanUpdaters() throws ScriptParsingException {
		assertSuccess(false, "boolean x = false ,, x &&= false ,, x");
		assertSuccess(false, "boolean x = false ,, x &&= true  ,, x");
		assertSuccess(false, "boolean x = true  ,, x &&= false ,, x");
		assertSuccess(true,  "boolean x = true  ,, x &&= true  ,, x");

		assertSuccess(false, "boolean x = false ,, x ||= false ,, x");
		assertSuccess(true,  "boolean x = false ,, x ||= true  ,, x");
		assertSuccess(true,  "boolean x = true  ,, x ||= false ,, x");
		assertSuccess(true,  "boolean x = true  ,, x ||= true  ,, x");

		assertSuccess(false, "boolean x = false ,, x ##= false ,, x");
		assertSuccess(true,  "boolean x = false ,, x ##= true  ,, x");
		assertSuccess(true,  "boolean x = true  ,, x ##= false ,, x");
		assertSuccess(false, "boolean x = true  ,, x ##= true  ,, x");

		assertSuccess(0,
			"""
			int y = 0
			boolean x = false
			x &&= ( y = 1 ,, true )
			y
			"""
		);
		assertSuccess(1,
			"""
			int y = 0
			boolean x = true
			x &&= ( y = 1 ,, true )
			y
			"""
		);
		assertSuccess(0,
			"""
			int y = 0
			boolean x = true
			x ||= ( y = 1 ,, false )
			y
			"""
		);
		assertSuccess(1,
			"""
			int y = 0
			boolean x = false
			x ||= ( y = 1 ,, true )
			y
			"""
		);
	}

	@Test
	public void testFailedUpdaters() throws ScriptParsingException {
		assertFail("Cannot bitwise and primitive int and primitive double", "int x = 3 ,, x &= 6.0L ,, x");
		assertFail("Cannot bitwise or primitive int and primitive double", "int x = 3 ,, x |= 6.0L ,, x");
		assertFail("Cannot bitwise xor primitive int and primitive double", "int x = 3 ,, x #= 6.0L ,, x");
		assertFail("Cannot bitwise and primitive double and primitive byte", "double x = 3 ,, x &= 6 ,, x");
		assertFail("Cannot bitwise or primitive double and primitive byte", "double x = 3 ,, x |= 6 ,, x");
		assertFail("Cannot bitwise xor primitive double and primitive byte", "double x = 3 ,, x #= 6 ,, x");
		assertFail("Cannot bitwise and primitive double and primitive double", "double x = 3 ,, x &= 6.0L ,, x");
		assertFail("Cannot bitwise or primitive double and primitive double", "double x = 3 ,, x |= 6.0L ,, x");
		assertFail("Cannot bitwise xor primitive double and primitive double", "double x = 3 ,, x #= 6.0L ,, x");

		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x &&= 0 ,, x");
		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x &&= false ,, x");
		assertFail("Can't implicitly cast primitive byte to primitive boolean", "boolean x = false ,, x &&= 0 ,, x");
		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x ||= 0 ,, x");
		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x ||= false ,, x");
		assertFail("Can't implicitly cast primitive byte to primitive boolean", "boolean x = false ,, x ||= 0 ,, x");
		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x ##= 0 ,, x");
		assertFail("Can't implicitly cast primitive int to primitive boolean", "int x = 0 ,, x ##= false ,, x");
		assertFail("Can't implicitly cast primitive byte to primitive boolean", "boolean x = false ,, x ##= 0 ,, x");

		assertFail("Cannot bitwise and primitive int and primitive boolean", "int x = 0 ,, x &= false ,, x");
		assertFail("Cannot bitwise and primitive boolean and primitive byte", "boolean x = false ,, x &= 0 ,, x");
		assertFail("Cannot bitwise or primitive int and primitive boolean", "int x = 0 ,, x |= false ,, x");
		assertFail("Cannot bitwise or primitive boolean and primitive byte", "boolean x = false ,, x |= 0 ,, x");
		assertFail("Cannot bitwise xor primitive int and primitive boolean", "int x = 0 ,, x #= false ,, x");
		assertFail("Cannot bitwise xor primitive boolean and primitive byte", "boolean x = false ,, x #= 0 ,, x");
	}
}