package builderb0y.scripting.parsing;

import java.util.function.Function;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.HashCommon;
import org.junit.jupiter.api.Test;

import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static org.junit.jupiter.api.Assertions.*;

/**
for anyone wondering, the reason why all the successful tests have
unnecessary spaces in them is to verify that spaces are ignored properly.
please do not use these tests as a style guide.
*/
public class ExpressionParserTest {

	@Test
	public void testBasicSyntax() throws ScriptParsingException {
		assertFail("Unexpected end of input", "");
		assertSuccess(1, "1");
		assertSuccess(1, "1.0");
		assertFail("Expected fractional part of number", "1.");
		assertFail("Unknown prefix operator: .", ".1");
		assertFail("Unknown prefix operator: .", ".1.0");
		assertFail("Multiple radix points", "1.0.");
		assertFail("Multiple radix points", "1.0.0");
		assertSuccess(256, "2x1p8");
		assertSuccess(256.0D, "2x1.0p8");
		assertSuccess(1.0D / 256.0D, "2x1p-8");
		assertSuccess(1.0D / 256.0D, "2x1.0p-8");
		assertSuccess(2, "1 + 1");
		assertSuccess(6, "2 + 2 * 2");
		assertSuccess(6, "2 * 2 + 2");
		assertSuccess(8, "2 * 2 + 2 * 2");
		assertSuccess(8, "2 + 2 * 2 + 2");
		assertSuccess(6, "2 + 2 + 2");
		assertSuccess(8, "2 * 2 * 2");
		assertSuccess(8, "( 2 + 2 ) * 2");
		assertSuccess(8, "2 * ( 2 + 2 )");
		assertFail("Unexpected end of input", "(");
		assertFail("Expected ')'", "(2");
		assertFail("Unexpected end of input", "(2 +");
		assertFail("Expected ')'", "(2 + 2");
		assertFail("Expected ')'", "((2 + 2)");
		assertFail("Unexpected character: )", ")");
		assertFail("Unexpected trailing character: )", "2)");
		assertFail("Unexpected trailing character: )", "+ 2)");
		assertFail("Unexpected trailing character: )", "2 + 2)");
		assertFail("Unexpected trailing character: )", "(2 + 2))");
		assertSuccess(256, "2 ^ 2 ^ 3");
		assertSuccess(1, "4 / 2 ^ 2");
		assertSuccess(-1, "-1 / 4"); //assert rounding towards -∞.
		assertSuccess(5, "sqrt ( 3 ^ 2 + 4 ^ 2 )");
		assertSuccess(5, "`sqrt` ( 3 ^ 2 + 4 ^ 2)");
		assertFail(
			"""
			Unknown variable: sqrt
			Candidates:
				Function sqrt: functionInvokeStatic: public static pure java/lang/Math.sqrt(D)D
			Actual form: sqrt""",
			"sqrt"
		);
		assertFail("Not a statement", "2 3");
		assertFail("Unreachable statement", "return(2) return(3)");
		assertFail("Not a statement", "int x = 2 ,, x ,, x");
	}

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
	public void testVar() throws ScriptParsingException {
		assertSuccess(2,
			"""
			var a = 3
			var b = 6
			byte c = a & b ;will only work if a and b are bytes.
			c
			"""
		);
	}

	@Test
	public void testFlow() throws ScriptParsingException {
		assertFail("Unreachable statement", "if (yes: return(0)) else (return(1)) return(2)");
		assertSuccess(0, "return ( if ( yes : return ( 0 ) ) else ( return ( 1 ) ) )");
		assertSuccess(1, "if ( yes : noop ) ,, 1");
		assertSuccess(1, "if ( yes : return ( 1 ) ) ,, 2");
		assertSuccess(1, "if ( yes : return ( 1 ) ) ,, return ( 2 )");
		assertFail("Not a statement", "if (yes: 1) 2");
		assertFail("Not a statement", "if (yes: 1) else (2) 3");
		assertSuccess(10,
			"""
			int sum = 0
			int counter = 1
			while ( counter <= 4 :
				sum = sum + counter
				counter = counter + 1
			)
			sum
			"""
		);
		assertSuccess(1,
			"""
			int local = 0
			local = local + 1
			if ( local == 1 :
				return ( 1 )
			)
			else (
				return ( 2 )
			)
			"""
		);
		assertSuccess(1,
			"""
			return(
				if ( yes : return ( 1 ) )
				else ( 2 )
			)
			"""
		);
		assertSuccess(10,
			"""
			int sum = 0
			int counter = 1
			while ( ( int tmp = counter * counter tmp < 25 ) :
				int tmp = sum + counter
				sum = tmp
				tmp = counter + 1
				counter = tmp
			)
			int tmp = sum
			tmp
			"""
		);
		assertSuccess(1,
			"""
			int counter = 0
			while ( yes :
				if ( counter == 10 : return ( 1 ) )
				counter = counter + 1
			) ,,
			-1
			"""
		);
		assertSuccess(10,
			"""
			int low = 0
			int high = 100
			while ( yes :
				int mid = ( low + high ) >> 1
				int square = mid ^ 2
				if ( square > 100 : high = mid )
				else if ( square < 100 : low = mid )
				else return ( mid )
			) ,,
			-1
			"""
		);
		assertSuccess(1,
			"""
			int counter = 0
			int loop = 0
			do while ( loop < 0 :
				++ counter
			)
			counter
			"""
		);
		assertSuccess(1,
			"""
			int counter = 0
			int loop = 0
			do until ( loop == 0 :
				++ counter
			)
			counter
			"""
		);
		assertSuccess(5,
			"""
			int sum = 0
			repeat ( 5 :
				++ sum
			)
			sum
			"""
		);
		assertSuccess(125,
			"""
			int sum = 0
			repeat ( 5 :
				repeat ( 5 :
					repeat ( 5 :
						sum = sum + 1
					)
				)
			)
			sum
			"""
		);
		assertSuccess(11,
			"""
			switch ( 1 :
				case ( 0 : 10 )
				case ( 1 : 11 )
				case ( 2 : 12 )
				default ( -1 )
			)
			"""
		);
		assertSuccess(-1,
			"""
			switch ( 3 :
				case ( 0 : 10 )
				case ( 1 : 11 )
				case ( 2 : 12 )
				default ( -1 )
			)
			"""
		);
		assertSuccess(10,
			"""
			switch ( 2 :
				case ( 0 : -1 )
				case ( 1 , 2 , 3 : 10 )
				case ( 4 , 5 : -2 )
				default ( -3 )
			)
			"""
		);
		assertFail("Switch must have at least one case", "switch (0: ) 1");
		assertFail("Switch must have at least one case", "switch (0: default (1))");
		assertFail("Switch value must be single-width int", "switch (1.0: case (1: noop))");
		assertFail("Switch value must be single-width int", "switch ('hi': case (1: noop))");
		assertSuccess(15,
			"""
			int sum = 0
			for ( int tmp = 1 , tmp <= 5 , ++ tmp :
				sum += tmp
			)
			sum
			"""
		);
		assertSuccess(15,
			"""
			int sum = 0
			for ( int forwards = 0 int backwards = 5 , forwards <= 5 , ++ forwards -- backwards :
				sum += backwards
			)
			sum
			"""
		);
		assertSuccess(1,
			"""
			switch ( int value = 5 value :
				case ( 0 : 0 )
				case ( 1 : 1 )
				default ( value & 1 )
			)
			"""
		);
		assertFail("Not a statement",
			"""
			switch (int value = 5,, value:
				case (0: 0)
				case (1: 1)
				default (value & 1)
			)
			value
			"""
		);
		assertSuccess(true,
			"""
			List list = ArrayList . new ( 5 )
			list . add ( 1 )
			list . add ( 2 )
			list . add ( 3 )
			list . add ( 4 )
			list . add ( 5 )
			for ( byte value in list :
				if ( value == 3 : return ( true ) )
			)
			return ( false )
			"""
		);
		assertSuccess(3,
			"""
			int result = 0
			if ( false : result = 1 )
			else result = 2 result = 3
			result
			"""
		);
		assertSuccess(3,
			"""
			int result = 0
			if ( false : result = 1 )
			else result = 2 ,, result = 3
			result
			"""
		);
		assertSuccess(5,
			"""
			int result = 0
			block (
				++ result
				if ( result < 5 : continue ( ) )
			)
			result
			"""
		);
		assertSuccess(5,
			"""
			int result = 0
			while ( true :
				++ result
				if ( result >= 5 : break ( ) )
			)
			result
			"""
		);
		assertSuccess(2 + 4 + 6 + 8 + 10,
			"""
			int sum = 0
			for ( int x = 0 , x <= 10 , ++ x :
				if ( x & 1 != 0 : continue ( ) )
				sum += x
			)
			sum
			"""
		);
	}

	@Test
	public void testBoolDotIf() throws ScriptParsingException {
		assertSuccess(true,
			"""
			boolean a = true
			boolean b = false
			return ( a . if ( b = true ) && b )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = true
			boolean b = false
			return ( a . unless ( b = true ) && ! b )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = false
			boolean b = false
			return ( ! a . if ( b = true ) && ! b )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = false
			boolean b = false
			return ( ! a . unless ( b = true ) && b )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = true
			boolean b = false
			boolean c = false
			return ( a . if ( b = true ) else ( c = true ) && b && ! c )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = true
			boolean b = false
			boolean c = false
			return ( a . unless ( b = true ) else ( c = true ) && ! b && c )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = false
			boolean b = false
			boolean c = false
			return ( ! a . if ( b = true ) else ( c = true ) && ! b && c )
			"""
		);
		assertSuccess(true,
			"""
			boolean a = false
			boolean b = false
			boolean c = false
			return ( ! a . unless ( b = true ) else ( c = true ) && b && ! c )
			"""
		);
	}

	@Test
	public void testDuplicateVariables() {
		assertFail("Variable 'tmp' is already defined in this scope", "int tmp = 1 ,, int tmp = 2 ,, 3");
		assertFail("Variable 'tmp' is already defined in this scope", "int tmp = 1 ,, ( int tmp = 2 ) ,, 3");
	}

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
		assertFail("Cannot bitwise and primitive I and primitive D", "int x = 3 ,, x &= 6.0L ,, x");
		assertFail("Cannot bitwise or primitive I and primitive D", "int x = 3 ,, x |= 6.0L ,, x");
		assertFail("Cannot bitwise xor primitive I and primitive D", "int x = 3 ,, x #= 6.0L ,, x");
		assertFail("Cannot bitwise and primitive D and primitive B", "double x = 3 ,, x &= 6 ,, x");
		assertFail("Cannot bitwise or primitive D and primitive B", "double x = 3 ,, x |= 6 ,, x");
		assertFail("Cannot bitwise xor primitive D and primitive B", "double x = 3 ,, x #= 6 ,, x");
		assertFail("Cannot bitwise and primitive D and primitive D", "double x = 3 ,, x &= 6.0L ,, x");
		assertFail("Cannot bitwise or primitive D and primitive D", "double x = 3 ,, x |= 6.0L ,, x");
		assertFail("Cannot bitwise xor primitive D and primitive D", "double x = 3 ,, x #= 6.0L ,, x");

		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x &&= 0 ,, x");
		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x &&= false ,, x");
		assertFail("Can't implicitly cast primitive B to primitive Z", "boolean x = false ,, x &&= 0 ,, x");
		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x ||= 0 ,, x");
		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x ||= false ,, x");
		assertFail("Can't implicitly cast primitive B to primitive Z", "boolean x = false ,, x ||= 0 ,, x");
		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x ##= 0 ,, x");
		assertFail("Can't implicitly cast primitive I to primitive Z", "int x = 0 ,, x ##= false ,, x");
		assertFail("Can't implicitly cast primitive B to primitive Z", "boolean x = false ,, x ##= 0 ,, x");

		assertFail("Cannot bitwise and primitive I and primitive Z", "int x = 0 ,, x &= false ,, x");
		assertFail("Cannot bitwise and primitive Z and primitive B", "boolean x = false ,, x &= 0 ,, x");
		assertFail("Cannot bitwise or primitive I and primitive Z", "int x = 0 ,, x |= false ,, x");
		assertFail("Cannot bitwise or primitive Z and primitive B", "boolean x = false ,, x |= 0 ,, x");
		assertFail("Cannot bitwise xor primitive I and primitive Z", "int x = 0 ,, x #= false ,, x");
		assertFail("Cannot bitwise xor primitive Z and primitive B", "boolean x = false ,, x #= 0 ,, x");
	}

	@Test
	public void testInstanceOf() throws ScriptParsingException {
		assertSuccess(true, "'hi' . is ( String )");
		assertSuccess(false, "null . is ( String )");
		assertSuccess(false, "'hi' . isnt ( String )");
		assertSuccess(true, "null . isnt ( String )");
		assertSuccess("5", "5 . as ( String )");
	}

	@Test
	public void testUserDefinedFunctions() throws ScriptParsingException {
		//assertSuccess(4, "int square ( int number : number * number ) ,, square ( 2 )");
		assertSuccess(4, "return ( int x = 2 ,, int square ( : x * x ) ,, square ( ) )");
		assertSuccess(1,
			"""
			boolean isPrime ( int number :
				for ( int test = 2 ,, int max = int ( sqrt ( number ) ) , test < max , ++ test :
					if ( number % test == 0 : return ( false ) )
				)
				return ( true )
			)
			
			isPrime ( 5 ) ? 1.0L : 0.0L
			"""
		);
		assertSuccess(1,
			"""
			int x = 2
			void setTo3 ( :
				x = 3
			)
			setTo3 ( )
			return ( x == 2 ? 1.0L : 0.0L )
			"""
		);
		assertSuccess(6,
			"""
			return ( 2 + (
				int x = 2
				int square ( int number : number * number )
				square ( x )
			) )
			"""
		);
		assertSuccess(1,
			"""
			int x = 2
			int squareX ( : x = 4 ,, x * x )
			int squared = squareX ( )
			return ( squared == 16 && x == 2 ? 1.0L : 0.0L )
			"""
		);
		assertFail("Variable 'a' is already defined in this scope", "int a = 0 ,, int f ( int a : a ) ,, f ( 0 )");
		assertFail("Variable 'a' is already defined in this scope", "int a = 0 ,, int f ( : int a = 0 ,, a ) ,, f ( 0 )");
	}

	@Test
	public void testUserDefinedClasses() throws ScriptParsingException {
		assertSuccess(2,
			"""
			class XYZ (
				int x
				int y
				int z
			)
			
			XYZ xyz = XYZ . new ( 1 , 2 , 3 )
			xyz . y
			"""
		);
		assertSuccess(42,
			"""
			class XYZ (
				int x
				int y = 42
				int z
			)
			XYZ xyz = XYZ . new ( )
			xyz . y
			"""
		);
		assertSuccess(2,
			"""
			class XYZ (
				int x
				int y = 42
				int z
			)
			XYZ xyz = XYZ . new ( 1 , 2 )
			xyz . z
			"""
		);
		assertSuccess("XYZ(x: 1, y: 2, z: 3)",
			"""
			class XYZ ( int x ,, int y ,, int z )
			XYZ . new ( 1 , 2 , 3 ) . toString ( )
			"""
		);
		assertSuccess(0,
			"""
			class Empty ( )
			Empty . new ( ) . hashCode ( )
			"""
		);
		assertSuccess(HashCommon.mix(HashCommon.mix(3) + 4),
			"""
			class XY ( int x ,, int y )
			XY . new ( 3 , 4 ) . hashCode ( )
			"""
		);
		assertSuccess(true,
			"""
			class Empty ( )
			Empty . new ( ) == Empty . new ( )
			"""
		);
		assertSuccess(true,
			"""
			class One ( int x )
			One . new ( 2 ) == One . new ( 2 )
			"""
		);
		assertSuccess(false,
			"""
			class One ( int x )
			One . new ( 2 ) == One . new ( 4 )
			"""
		);
		assertSuccess(true,
			"""
			class Two ( int x ,, int y )
			Two . new ( 2 , 4 ) == Two . new ( 2 , 4 )
			"""
		);
		assertSuccess(false,
			"""
			class Two ( int x ,, int y )
			Two . new ( 2 , 4 ) == Two . new ( 4 , 2 )
			"""
		);
	}

	@Test
	void testVariablesInFunctions() throws ScriptParsingException {
		assertSuccess(1,
			"""
			int a(:
				int x = 1
				return(x)
			)
			return(a())
			"""
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedFunctions() throws ScriptParsingException {
		Object expected = new Object();
		Object actual = (
			new ScriptParser<>(
				Function.class,
				"""
				Object a ( :
					Object b ( :
						return ( object )
					)
					return ( b ( ) )
				)
				return ( a ( ) )
				"""
			)
			.addEnvironment(
				new MutableScriptEnvironment()
				.addVariableLoad("object", 1, TypeInfos.OBJECT)
			)
			.parse()
			.apply(expected)
		);
		assertEquals(expected, actual);
	}

	@Test
	public void testFunctionsWithSameParameterNames() throws ScriptParsingException {
		assertSuccess(1,
			"""
			int a ( int x :
				return ( x )
			)
			
			int b ( int x :
				return ( x )
			)
			
			return ( b ( a ( 1 ) ) )
			"""
		);
	}

	@Test
	public void testFunctionsCapturingWithOffset() throws ScriptParsingException {
		assertSuccess(1,
			"""
			block (
				float x = 2
			)
			int x = 1
			int get(:
				return(x)
			)
			return(get())
			"""
		);
	}

	@Test
	public void testDescriptiveIdentifierErrorMessages() {
		assertFail(
			"""
			Unknown variable: exp
			Candidates:
				Function exp: functionInvokeStatic: public static pure java/lang/Math.exp(D)D
			Actual form: exp""",
			"exp"
		);
		assertFail(
			"""
			Unknown function or incorrect arguments: e
			Candidates:
				Variable e: ConstantInsnTree of type primitive D (constant: 2.718281828459045 of type primitive D)
			Actual form: e()""",
			"e ( )"
		);
		assertFail(
			"""
			Unknown field: getKey
			Candidates:
				Method interface java/util/Map$Entry extends java/lang/Object.getKey: methodInvoke: public abstract java/util/Map$Entry.getKey()Ljava/lang/Object; (interface)
			Actual form: LoadInsnTree of type interface java/util/Map$Entry extends java/lang/Object (not constant).getKey""",
			"MapEntry entry = null ,, entry . getKey"
		);
		assertFail(
			"""
			Unknown method or incorrect arguments: key
			Candidates:
				Field interface java/util/Map$Entry extends java/lang/Object.key: fieldInvoke: public abstract java/util/Map$Entry.getKey()Ljava/lang/Object; (interface)
			Actual form: LoadInsnTree of type interface java/util/Map$Entry extends java/lang/Object (not constant).key()""",
			"MapEntry entry = null ,, entry . key ( )"
		);
	}

	@Test
	public void testPrint() throws ScriptParsingException {
		evaluate("print ( 'a: ', 1, ', b: ' , 2 ) ,, 0");
		assertSuccess("$10", "'$$10'");
		assertSuccess("a: 1, b: 2", "'a: $1, b: $2'");
		assertSuccess("a: 1, b: 2", "'a: $.1, b: $.2'");
		assertSuccess("a: 1, b: 2", "'a: $(1), b: $(2)'");
		assertSuccess("a: 1, b: 2", "'a: $.(1), b: $.(2)'");
		assertSuccess("a: 1, b: 2", "int a = 1 ,, int b = 2 ,, 'a: $a, b: $b'");
		assertSuccess("2^2 = 4", "int square ( int x : x * x ) ,, int x = 2 ,, '$x^2 = $square ( x )'");
		assertSuccess("a 42 b", "class X(int x) 'a $.X.new(42).x b'");
	}

	public static void assertSuccess(Object expected, String script) throws ScriptParsingException {
		Object actual = evaluate(script);
		if (expected instanceof Number a && actual instanceof Number b) {
			assertEquals(a.doubleValue(), b.doubleValue());
		}
		else {
			assertEquals(expected, actual);
		}
	}

	public static void assertFail(String message, String script) throws AssertionError {
		try {
			fail(String.valueOf(evaluate(script)));
		}
		catch (ScriptParsingException expected) {
			assertEquals(message, expected.getMessage());
		}
	}

	public static Object evaluate(String input) throws ScriptParsingException {
		return (
			new ScriptParser<>(Supplier.class, input)
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.addEnvironment(JavaUtilScriptEnvironment.ALL)
			.parse()
			.get()
		);
	}
}