package builderb0y.bigglobe.scripting;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.*;

import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.scripting.ScriptInterfaces.ObjectSupplier;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class NBTScriptEnvironmentTest {

	@Test
	public void testConstructors() throws ScriptParsingException {
		NbtCompound expected = new NbtCompound();
		expected.put("byte", NbtByte.of((byte)(42)));
		expected.put("short", NbtShort.of((short)(42)));
		expected.put("int", NbtInt.of((int)(42)));
		expected.put("long", NbtLong.of((long)(42)));
		expected.put("float", NbtFloat.of((float)(42)));
		expected.put("double", NbtDouble.of((double)(42)));
		expected.put("string", NbtString.of("42"));
		expected.put("byteArray0", new NbtByteArray(new byte[] {}));
		expected.put("byteArray1", new NbtByteArray(new byte[] { 42 }));
		expected.put("byteArray2", new NbtByteArray(new byte[] { 42, 123 }));
		expected.put("intArray0", new NbtIntArray(new int[] {}));
		expected.put("intArray1", new NbtIntArray(new int[] { 42 }));
		expected.put("intArray2", new NbtIntArray(new int[] { 42, 123 }));
		expected.put("longArray0", new NbtLongArray(new long[] {}));
		expected.put("longArray1", new NbtLongArray(new long[] { 42 }));
		expected.put("longArray2", new NbtLongArray(new long[] { 42, 123 }));
		expected.put("list0", listOf());
		expected.put("list1", listOf(NbtByte.of((byte)(42))));
		expected.put("list2", listOf(NbtByte.of((byte)(42)), NbtByte.of((byte)(123))));
		NbtCompound nestedCompound = new NbtCompound();
		nestedCompound.putByte("a", (byte)(1));
		nestedCompound.putByte("b", (byte)(2));
		expected.put("compound", nestedCompound);
		assertSuccess(expected,
			"""
			nbtCompound (
				byte : nbtByte ( 42 ) ,
				short : nbtShort ( 42 ) ,
				int : nbtInt ( 42 ) ,
				long : nbtLong ( 42 ) ,
				float : nbtFloat ( 42 ) ,
				double : nbtDouble ( 42 ) ,
				string : nbtString ( '42' ) ,
				byteArray0 : nbtByteArray ( ) ,
				byteArray1 : nbtByteArray ( 42 ) ,
				byteArray2 : nbtByteArray ( 42 , 123 ) ,
				intArray0 : nbtIntArray ( ) ,
				intArray1 : nbtIntArray ( 42 ) ,
				intArray2 : nbtIntArray ( 42 , 123 ) ,
				longArray0 : nbtLongArray ( ) ,
				longArray1 : nbtLongArray ( 42 ) ,
				longArray2 : nbtLongArray ( 42 , 123 ) ,
				list0 : nbtList ( ) ,
				list1 : nbtList ( nbtByte ( 42 ) ),
				list2 : nbtList ( nbtByte ( 42 ), nbtByte ( 123 ) ),
				compound : nbtCompound (
					a : nbtByte ( 1 ) ,
					b : nbtByte ( 2 )
				)
			)
			"""
		);
	}

	@Test
	public void testCasting() throws ScriptParsingException {
		assertSuccess((byte)(42), "nbtByte ( 42 ) . asByte ( )");
		assertSuccess((short)(42), "nbtShort ( 42 ) . asShort ( )");
		assertSuccess((int)(42), "nbtInt ( 42 ) . asInt ( )");
		assertSuccess((long)(42), "nbtLong ( 42 ) . asLong ( )");
		assertSuccess((float)(42), "nbtFloat ( 42 ) . asFloat ( )");
		assertSuccess((double)(42), "nbtDouble ( 42 ) . asDouble ( )");
		assertSuccess("42", "nbtString ( '42' ) . asString ( )");
		assertSuccess((byte)(0), "NbtByte ( null ) . asByte ( )");
	}

	@Test
	public void testMembers() throws ScriptParsingException {
		assertSuccess(1, "nbtCompound ( a : nbtByte ( 1 ) , b : nbtByte ( 2 ) ) . a . asInt ( )");
		assertSuccess(null, "nbtCompound ( ) . missing");
		assertSuccess(null, "nbtByte ( 0 ) . notACompound");
		assertSuccess(null, "nbtByteArray ( 1 , 2 ) . ( -1 )");
		assertSuccess((byte)(1), "nbtByteArray ( 1 , 2 ) . ( 0 ) . asByte ( )");
		assertSuccess((byte)(2), "nbtByteArray ( 1 , 2 ) . ( 1 ) . asByte ( )");
		assertSuccess(null, "nbtByteArray ( 1 , 2 ) . ( 2 )");
	}

	@Test
	public void testMemberAssignment() throws ScriptParsingException {
		assertSuccess(
			compound(c -> c.putByte("a", (byte)(1))),
			"""
			var c = nbtCompound ( )
			c . a = nbtByte ( 1 )
			c
			"""
		);
		assertSuccess(NbtByte.of((byte)(1)),
			"""
			Nbt nbt = nbtCompound()
			nbt.a = 1
			nbt.a
			"""
		);
		assertSuccess(NbtByte.of((byte)(1)),
			"""
			Nbt nbt = nbtCompound()
			nbt.('a') = 1
			nbt.a
			"""
		);
	}

	public static NbtList listOf(NbtElement... elements) {
		NbtList list = new NbtList();
		list.addAll(Arrays.asList(elements));
		return list;
	}

	public static NbtCompound compound(Consumer<NbtCompound> initializer) {
		NbtCompound compound = new NbtCompound();
		initializer.accept(compound);
		return compound;
	}

	public static void assertSuccess(Object expected, String script) throws ScriptParsingException {
		assertEquals(expected, evaluate(script));
	}

	public static void assertFail(String script) {
		try {
			fail(String.valueOf(evaluate(script)));
		}
		catch (Exception expected) {}
	}

	public static Object evaluate(String script) throws ScriptParsingException {
		return new ScriptParser<>(ObjectSupplier.class, script).addEnvironment(MathScriptEnvironment.INSTANCE).addEnvironment(NbtScriptEnvironment.INSTANCE).parse().getAsObject();
	}
}