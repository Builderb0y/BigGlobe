package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class CastingTest extends TestCommon {

	@Test
	public void testCasting() throws ScriptParsingException {
		assertSuccess(Integer.MAX_VALUE, "double value = +10000000000000000000000000000000000.0 ,, int ( value )");
		assertSuccess(Integer.MIN_VALUE, "double value = -10000000000000000000000000000000000.0 ,, int ( value )");
		assertSuccess(Integer.MIN_VALUE, "double value = -2147483647.5L ,, int ( value )");
		assertSuccess(Integer.MIN_VALUE, "double value = -2147483648.0L ,, int ( value )");
		assertSuccess(Integer.MIN_VALUE, "double value = -2147483648.5L ,, int ( value )");
		assertSuccess(Integer.MIN_VALUE, "double value = -2147483649.0L ,, int ( value )");

		assertSuccess(-1,  "float  value = -1.5 ,, truncInt  ( value )");
		assertSuccess(-1,  "double value = -1.5 ,, truncInt  ( value )");
		assertSuccess(-1L, "float  value = -1.5 ,, truncLong ( value )");
		assertSuccess(-1L, "double value = -1.5 ,, truncLong ( value )");
		assertSuccess( 1,  "float  value =  1.5 ,, truncInt  ( value )");
		assertSuccess( 1,  "double value =  1.5 ,, truncInt  ( value )");
		assertSuccess( 1L, "float  value =  1.5 ,, truncLong ( value )");
		assertSuccess( 1L, "double value =  1.5 ,, truncLong ( value )");

		assertSuccess(-2,  "float  value = -1.5 ,, floorInt  ( value )");
		assertSuccess(-2,  "double value = -1.5 ,, floorInt  ( value )");
		assertSuccess(-2L, "float  value = -1.5 ,, floorLong ( value )");
		assertSuccess(-2L, "double value = -1.5 ,, floorLong ( value )");
		assertSuccess( 1,  "float  value =  1.5 ,, floorInt  ( value )");
		assertSuccess( 1,  "double value =  1.5 ,, floorInt  ( value )");
		assertSuccess( 1L, "float  value =  1.5 ,, floorLong ( value )");
		assertSuccess( 1L, "double value =  1.5 ,, floorLong ( value )");

		assertSuccess(-1,  "float  value = -1.5 ,, ceilInt  ( value )");
		assertSuccess(-1,  "double value = -1.5 ,, ceilInt  ( value )");
		assertSuccess(-1L, "float  value = -1.5 ,, ceilLong ( value )");
		assertSuccess(-1L, "double value = -1.5 ,, ceilLong ( value )");
		assertSuccess( 2,  "float  value =  1.5 ,, ceilInt  ( value )");
		assertSuccess( 2,  "double value =  1.5 ,, ceilInt  ( value )");
		assertSuccess( 2L, "float  value =  1.5 ,, ceilLong ( value )");
		assertSuccess( 2L, "double value =  1.5 ,, ceilLong ( value )");

		assertSuccess(-1,  "float  value = -1.25 ,, roundInt  ( value )");
		assertSuccess(-1,  "double value = -1.25 ,, roundInt  ( value )");
		assertSuccess(-1L, "float  value = -1.25 ,, roundLong ( value )");
		assertSuccess(-1L, "double value = -1.25 ,, roundLong ( value )");
		assertSuccess( 1,  "float  value =  1.25 ,, roundInt  ( value )");
		assertSuccess( 1,  "double value =  1.25 ,, roundInt  ( value )");
		assertSuccess( 1L, "float  value =  1.25 ,, roundLong ( value )");
		assertSuccess( 1L, "double value =  1.25 ,, roundLong ( value )");

		assertSuccess(-1,  "float  value = -1.5 ,, roundInt  ( value )");
		assertSuccess(-1,  "double value = -1.5 ,, roundInt  ( value )");
		assertSuccess(-1L, "float  value = -1.5 ,, roundLong ( value )");
		assertSuccess(-1L, "double value = -1.5 ,, roundLong ( value )");
		assertSuccess( 2,  "float  value =  1.5 ,, roundInt  ( value )");
		assertSuccess( 2,  "double value =  1.5 ,, roundInt  ( value )");
		assertSuccess( 2L, "float  value =  1.5 ,, roundLong ( value )");
		assertSuccess( 2L, "double value =  1.5 ,, roundLong ( value )");

		assertSuccess(-2,  "float  value = -1.75 ,, roundInt  ( value )");
		assertSuccess(-2,  "double value = -1.75 ,, roundInt  ( value )");
		assertSuccess(-2L, "float  value = -1.75 ,, roundLong ( value )");
		assertSuccess(-2L, "double value = -1.75 ,, roundLong ( value )");
		assertSuccess( 2,  "float  value =  1.75 ,, roundInt  ( value )");
		assertSuccess( 2,  "double value =  1.75 ,, roundInt  ( value )");
		assertSuccess( 2L, "float  value =  1.75 ,, roundLong ( value )");
		assertSuccess( 2L, "double value =  1.75 ,, roundLong ( value )");
	}

	@Test
	public void testInstanceOf() throws ScriptParsingException {
		assertSuccess(true, "'hi' . is ( String )");
		assertSuccess(false, "null . is ( String )");
		assertSuccess(false, "'hi' . isnt ( String )");
		assertSuccess(true, "null . isnt ( String )");
		assertSuccess("5", "5 . as ( String )");
	}
}