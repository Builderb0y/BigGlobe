package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class BoolDotIfTest extends TestCommon {

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
}