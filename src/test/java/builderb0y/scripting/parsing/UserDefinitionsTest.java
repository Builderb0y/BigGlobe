package builderb0y.scripting.parsing;

import it.unimi.dsi.fastutil.HashCommon;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import builderb0y.scripting.ScriptInterfaces.ObjectUnaryOperator;
import builderb0y.scripting.TestCommon;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.ClassOptimizerTest.dumpBytecode;
import static org.junit.jupiter.api.Assertions.*;

public class UserDefinitionsTest extends TestCommon {

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
		assertSuccess(2,
			"""
			var * ( a = 3 , b = 6 )
			byte c = a & b
			c
			"""
		);
	}

	@Test
	public void testVoidVariables() throws ScriptParsingException {
		assertFail("void-typed variables are not allowed.", "void v = noop ,, v");
		assertFail("void-typed variables are not allowed.", "void * ( v = noop ) ,, v");
		assertFail("void-typed variables are not allowed.", "var v = noop ,, v");
		assertFail("void-typed variables are not allowed.", "var * ( v = noop ) ,, v");
		assertSuccess(0, "void nothing ( : noop ) nothing ( ) 0");
		assertFail("void-typed parameters are not allowed.", "void nothing ( void v : noop )");
		assertFail("void-typed parameters are not allowed.", "void nothing ( void * ( v ) : noop )");
	}

	@Test
	public void testDuplicateVariables() {
		assertFail("Variable 'tmp' is already defined in this scope", "int tmp = 1 ,, int tmp = 2 ,, 3");
		assertFail("Variable 'tmp' is already defined in this scope", "int tmp = 1 ,, ( int tmp = 2 ) ,, 3");
		assertFail("Variable 'tmp' is already defined in this scope", "int * ( tmp = 1 , tmp = 2 ) ,, 3");
	}

	@Test
	public void testSelfReferencingVariables() {
		assertFail("Variable 'x' has not been assigned to yet.", "int x = x");
		assertFail("Variable 'x' has not had its type inferred yet.", "var x = x");
		assertFail("Variable 'x' has not been assigned to yet.", "int * ( x = x )");
		assertFail("Variable 'x' has not had its type inferred yet.", "var * ( x = x )");
	}

	@Test
	public void testMultipleVariables() throws ScriptParsingException {
		assertSuccess(0,
			"""
			int * ( )
			0
			"""
		);
		assertSuccess(1,
			"""
			int * ( x = 1 )
			x
			"""
		);
		assertSuccess(3,
			"""
			int * ( x = 1 , y = 2 )
			x + y
			"""
		);
		assertSuccess(0,
			"""
			var * ( )
			0
			"""
		);
		assertSuccess(1,
			"""
			var * ( x = 1 )
			x
			"""
		);
		assertSuccess(3,
			"""
			var * ( x = 1 , y = 2 )
			x + y
			"""
		);
	}

	public static String unknown(String varName) {
		return "Unknown variable: " + varName + "\nCandidates:";
	}

	@Test
	public void testScopes() throws ScriptParsingException {
		assertFail(unknown("x"),
			"""
			if ( true : int x = 2 )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			if ( true : int x = 2 )
			else ( int y = 3 )
			x
			"""
		);
		assertFail(unknown("y"),
			"""
			if ( true : int x = 2 )
			else ( int y = 3 )
			y
			"""
		);
		assertFail(unknown("x"),
			"""
			unless ( false : int x = 2 )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			unless ( false : int x = 2 )
			else ( int y = 3 )
			x
			"""
		);
		assertFail(unknown("y"),
			"""
			unless ( false : int x = 2 )
			else ( int y = 3 )
			y
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			if ( b : int x = 2 )
			else ( print(x) )
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			unless ( b : int x = 2 )
			else ( print(x) )
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = false
			if ( b : int x = 2 )
			else ( print(x) )
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = false
			unless ( b : int x = 2 )
			else ( print(x) )
			"""
		);
		assertFail(unknown("x"),
			"""
			void consume( int x : noop )
			consume ( int y = 3 ,, y )
			x
			"""
		);
		assertFail(unknown("y"),
			"""
			void consume( int x : noop )
			consume ( int y = 3 ,, y )
			y
			"""
		);
		assertFail(unknown("x"),
			"""
			for ( int x = 0 , x < 5, ++ x : noop )
			x
			"""
		);
		assertFail(unknown("y"),
			"""
			for ( int x = 0 , x < 5, ++ x : int y = 0 )
			y
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			while ( b : int x = 2 b = false )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			until ( b : int x = 2 b = false )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			do while ( b : int x = 2 b = false )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			boolean b = true
			do until ( b : int x = 2 b = false )
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			block (
				int x = 2
			)
			x
			"""
		);
		assertFail(unknown("x"),
			"""
			switch ( 0 :
				case ( 0 : int x = 0 )
				default ( int y = 0 )
			)
			x
			"""
		);
		assertFail(unknown("y"),
			"""
			switch ( 0 :
				case ( 0 : int x = 0 )
				default ( int y = 0 )
			)
			y
			"""
		);
		assertFail(unknown("z"),
			"""
			switch ( int z = 0 ,, z :
				case ( 0 : int x = 0 )
				default ( int y = 0 )
			)
			z
			"""
		);
		assertSuccess(2,
			"""
			boolean condition = true
			if ( condition :
				int x = 2
			)
			else (
				int x = 2
			)
			2
			"""
		);
		assertSuccess(2,
			"""
			int s = 2
			switch ( s :
				case ( 1 : int x = 2 )
				case ( 2 : int x = 2 )
				case ( 3 : int x = 2 )
			)
			s
			"""
		);
	}

	@Test
	public void testUserDefinedFunctions() throws ScriptParsingException {
		assertSuccess(4, "int square ( int number : number * number ) ,, square ( 2 )");
		assertSuccess(4, "return ( int x = 2 ,, int square ( : x * x ) ,, square ( ) )");
		assertSuccess(1,
			"""
			boolean isPrime ( int number :
				for ( int test = 2 ,, int max = int ( sqrt ( number ) ) , test < max , ++ test :
					if ( number % test == 0 : return ( false ) )
				)
				return ( true )
			)
			
			isPrime ( 5 ) ? 1 : 0
			"""
		);
		assertSuccess(1,
			"""
			int x = 2
			void setTo3 ( :
				x = 3
			)
			setTo3 ( )
			return ( x == 2 ? 1 : 0 )
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
		assertFail("Variable 'a' has already been declared in this scope.", "int a = 0 ,, int f ( int a : a ) ,, f ( 0 )");
		assertFail("Variable 'a' is already defined in this scope", "int a = 0 ,, int f ( : int a = 0 ,, a ) ,, f ( 0 )");
		assertFail("Variable 'x' has not been assigned to yet.",
			"""
			int x = (
				int square(: x * x)
				square()
			)
			"""
		);
		assertFail("Variable 'x' has not had its type inferred yet.",
			"""
			var x = (
				int square(: x * x)
				square()
			)
			"""
		);
		assertSuccess(4,
			"""
			return(
				(
					int two(: 2)
					two()
				)
				+
				(
					int two(: 2)
					two()
				)
			)
			"""
		);
		assertSuccess(5,
			"""
			int sum ( int * ( a , b ) :
				a + b
			)
			sum ( 2 , 3 )
			"""
		);
		assertSuccess(0,
			"""
			int zero ( int * ( ) : 0 )
			zero ( )
			"""
		);
		assertSuccess(0,
			"""
			int result = 0
			int first ( : result )
			int second ( : first ( ) )
			second ( )
			"""
		);
		assertSuccess(0,
			"""
			int result = 0
			class Test ( )
			int Test . getResult ( : result )
			int getResult ( : Test . new ( ) . getResult ( ) )
			getResult ( )
			"""
		);
		assertSuccess(0,
			"""
			int result = 0
			class Test ( )
			int Test . getResult ( : result )
			Test test = new ( )
			int getResult ( : test . getResult ( ) )
			getResult ( )
			"""
		);
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
			class XYZ ( int x , int y , int z )
			XYZ . new ( 1 , 2 , 3 ) . toString ( )
			"""
		);
		assertSuccess(2,
			"""
			class XYZ ( int * ( x y z ) )
			
			XYZ xyz = XYZ . new ( 1 , 2 , 3 )
			xyz . y
			"""
		);
		assertSuccess(2,
			"""
			class XYZ ( int * ( x , y , z ) )
			
			XYZ xyz = XYZ . new ( 1 , 2 , 3 )
			xyz . y
			"""
		);
		assertSuccess(42,
			"""
			class XYZ ( int * ( x y = 42 z ) )
			XYZ xyz = XYZ . new ( )
			xyz . y
			"""
		);
		assertSuccess(42,
			"""
			class XYZ ( int * ( x , y = 42 , z ) )
			XYZ xyz = XYZ . new ( )
			xyz . y
			"""
		);
		assertSuccess(2,
			"""
			class XYZ ( int * ( x y = 42 z ) )
			XYZ xyz = XYZ . new ( 1 , 2 )
			xyz . z
			"""
		);
		assertSuccess(2,
			"""
			class XYZ ( int * ( x , y = 42 , z ) )
			XYZ xyz = XYZ . new ( 1 , 2 )
			xyz . z
			"""
		);
		assertSuccess("XYZ(x: 1, y: 2, z: 3)",
			"""
			class XYZ ( int x , int y , int z )
			XYZ . new ( 1 , 2 , 3 ) . toString ( )
			"""
		);
		assertSuccess("XYZ(x: 1, y: 2, z: 3)",
			"""
			class XYZ ( int * ( x y z ) )
			XYZ . new ( 1 , 2 , 3 ) . toString ( )
			"""
		);
		assertSuccess("XYZ(x: 1, y: 2, z: 3)",
			"""
			class XYZ ( int * ( x , y , z ) )
			XYZ . new ( 1 , 2 , 3 ) . toString ( )
			"""
		);
		assertSuccess(0,
			"""
			class Empty ( )
			Empty . new ( ) . hashCode ( )
			"""
		);
		assertSuccess(0,
			"""
			class Empty ( int * ( ) )
			Empty . new ( ) . hashCode ( )
			"""
		);
		assertSuccess(
			HashCommon.mix(HashCommon.mix(3) + 4),
			"""
			class XY ( int x , int y )
			XY . new ( 3 , 4 ) . hashCode ( )
			"""
		);
		assertSuccess(
			HashCommon.mix(HashCommon.mix(3) + 4),
			"""
			class XY ( int * ( x y ) )
			XY . new ( 3 , 4 ) . hashCode ( )
			"""
		);
		assertSuccess(
			HashCommon.mix(HashCommon.mix(3) + 4),
			"""
			class XY ( int * ( x , y ) )
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
		assertSuccess(true,
			"""
			class One ( int * ( x ) )
			One . new ( 2 ) == One . new ( 2 )
			"""
		);
		assertSuccess(false,
			"""
			class One ( int x )
			One . new ( 2 ) == One . new ( 4 )
			"""
		);
		assertSuccess(false,
			"""
			class One ( int * ( x ) )
			One . new ( 2 ) == One . new ( 4 )
			"""
		);
		assertSuccess(true,
			"""
			class Two ( int x , int y )
			Two . new ( 2 , 4 ) == Two . new ( 2 , 4 )
			"""
		);
		assertSuccess(false,
			"""
			class Two ( int x , int y )
			Two . new ( 2 , 4 ) == Two . new ( 4 , 2 )
			"""
		);
		assertSuccess(true,
			"""
			class Two ( int * ( x y ) )
			Two . new ( 2 , 4 ) == Two . new ( 2 , 4 )
			"""
		);
		assertSuccess(false,
			"""
			class Two ( int * ( x y ) )
			Two . new ( 2 , 4 ) == Two . new ( 4 , 2 )
			"""
		);
		assertSuccess(true,
			"""
			class Two ( int * ( x , y ) )
			Two . new ( 2 , 4 ) == Two . new ( 2 , 4 )
			"""
		);
		assertSuccess(false,
			"""
			class Two ( int * ( x , y ) )
			Two . new ( 2 , 4 ) == Two . new ( 4 , 2 )
			"""
		);
		assertSuccess(2,
			"""
			class One ( int x = 2 )
			One one = new ( )
			one . x
			"""
		);
		assertSuccess(2,
			"""
			class One ( int x = 1 )
			One one = new ( 2 )
			one . x
			"""
		);
		assertSuccess(4,
			"""
			return(
				(
					class C(int x)
					C.new(2).x
				)
				+
				(
					class C(int x)
					C.new(2).x
				)
			)
			"""
		);
	}

	@Test
	public void testExtensionMethods() throws ScriptParsingException {
		assertSuccess(2,
			"""
			class IntBox ( int x )
			void IntBox . increment ( : ++ this . x )
			IntBox box = new ( 1 )
			box . increment ( )
			return ( box . x )
			"""
		);
	}

	@Test
	public void testVariablesInFunctions() throws ScriptParsingException {
		assertSuccess(1,
			"""
			int a ( :
				int x = 1
				return ( x )
			)
			return ( a ( ) )
			"""
		);
	}

	@Test
	public void testNestedFunctions() throws ScriptParsingException {
		Object expected = new Object();
		Object actual = (
			new ScriptParser<>(
				ObjectUnaryOperator.class,
				"""
				Object a ( :
					Object b ( :
						return ( x )
					)
					return ( b ( ) )
				)
				return ( a ( ) )
				"""
			)
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				environment.addVariableLoad("x", TypeInfos.OBJECT);
			})
			.parse(new ScriptClassLoader())
			.applyAsObject(expected)
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
			int get ( :
				return ( x )
			)
			return ( get ( ) )
			"""
		);
	}

	@Test
	public void testRecursion() throws ScriptParsingException {
		assertSuccess(5 * 4 * 3 * 2 * 1,
			"""
			int factorial ( int x :
				return ( x <= 0 ? 1 : x * factorial ( x - 1 ) )
			)
			factorial ( 5 )
			"""
		);
		assertSuccess(5 + 4 + 3 + 2 + 1,
			"""
			int main ( int counter :
				int sub ( :
					return ( main ( counter - 1 ) )
				)
				return ( counter > 0 ? counter + sub ( ) : 0 )
			)
			main ( 5 )
			"""
		);
	}

	@Test
	@Disabled
	public void testCapturing() throws ScriptParsingException {
		dumpBytecode(
			"""
			int * ( x = 1 , y = 2 , z = 3 )
			int a ( :
				int b ( :
					int c ( :
						y
					)
					c ( )
				)
				b ( )
			)
			a ( )
			"""
		);
	}
}