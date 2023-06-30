package builderb0y.scripting.parsing;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.TestCommon;

public class ElvisTest extends TestCommon {

	@Test
	public void testElvis() throws ScriptParsingException {
		assertSuccess("a", "'a' ?: 'b'");
		assertSuccess("b", "null ?: 'b'");
		assertSuccess("b", "String a = null ,, a ?: 'b'");
	}

	@Test
	public void testNestedElvis() throws ScriptParsingException {
		assertSuccess(null, "String a = null,, String b = null,, String c = null,, a ?: b ?: c");
		assertSuccess("c" , "String a = null,, String b = null,, String c = 'c' ,, a ?: b ?: c");
		assertSuccess("b" , "String a = null,, String b = 'b' ,, String c = null,, a ?: b ?: c");
		assertSuccess("b" , "String a = null,, String b = 'b' ,, String c = 'c' ,, a ?: b ?: c");
		assertSuccess("a" , "String a = 'a' ,, String b = null,, String c = null,, a ?: b ?: c");
		assertSuccess("a" , "String a = 'a' ,, String b = null,, String c = 'c' ,, a ?: b ?: c");
		assertSuccess("a" , "String a = 'a' ,, String b = 'b' ,, String c = null,, a ?: b ?: c");
		assertSuccess("a" , "String a = 'a' ,, String b = 'b' ,, String c = 'c' ,, a ?: b ?: c");
	}

	@Test
	public void testMemberElvis() throws ScriptParsingException {
		assertSuccess(null, "class C(String s) C c = null c.?s");
		assertSuccess(null, "class C(String s) C c = C.new(null) c.?s");
		assertSuccess("a", "class C(String s) C c = C.new('a') c.?s");
		assertSuccess("a", "class C(String s) C c = null c.?s ?: 'a'");
		assertSuccess("a", "class C(String s) C c = C.new(null) c.?s ?: 'a'");
		assertSuccess("a", "class C(String s) C c = C.new('a') c.?s ?: 'b'");
		assertSuccess(Float.NaN, "class C(float f) C c = null c.?f");
		assertSuccess(Float.NaN, "class C(float f) C c = C.new(nan) c.?f");
		assertSuccess(1.0F, "class C(float f) C c = C.new(nan) Float(float(c.?f ?: 1.0))");
		assertSuccess(Double.NaN, "class C(double d) C c = null c.?d");
		assertSuccess(Double.NaN, "class C(double d) C c = C.new(nan) c.?d");
		assertSuccess(1.0D, "class C(double d) C c = C.new(nan) Double(double(c.?d ?: 1.0L))");
	}
}