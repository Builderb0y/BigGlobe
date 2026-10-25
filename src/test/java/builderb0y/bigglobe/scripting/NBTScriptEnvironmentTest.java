package builderb0y.bigglobe.scripting;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.*;

import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.scripting.ScriptInterfaces.ObjectSupplier;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

public class NBTScriptEnvironmentTest {

	@Test
	public void testConstructors() throws ScriptParsingException {
		CompoundTag expected = new CompoundTag();
		expected.put("byte", ByteTag.valueOf((byte)(42)));
		expected.put("short", ShortTag.valueOf((short)(42)));
		expected.put("int", IntTag.valueOf((int)(42)));
		expected.put("long", LongTag.valueOf((long)(42)));
		expected.put("float", FloatTag.valueOf((float)(42)));
		expected.put("double", DoubleTag.valueOf((double)(42)));
		expected.put("string", StringTag.valueOf("42"));
		expected.put("byteArray0", new ByteArrayTag(new byte[] {}));
		expected.put("byteArray1", new ByteArrayTag(new byte[] { 42 }));
		expected.put("byteArray2", new ByteArrayTag(new byte[] { 42, 123 }));
		expected.put("intArray0", new IntArrayTag(new int[] {}));
		expected.put("intArray1", new IntArrayTag(new int[] { 42 }));
		expected.put("intArray2", new IntArrayTag(new int[] { 42, 123 }));
		expected.put("longArray0", new LongArrayTag(new long[] {}));
		expected.put("longArray1", new LongArrayTag(new long[] { 42 }));
		expected.put("longArray2", new LongArrayTag(new long[] { 42, 123 }));
		expected.put("list0", listOf());
		expected.put("list1", listOf(ByteTag.valueOf((byte)(42))));
		expected.put("list2", listOf(ByteTag.valueOf((byte)(42)), ByteTag.valueOf((byte)(123))));
		CompoundTag nestedCompound = new CompoundTag();
		nestedCompound.putByte("a", (byte)(1));
		nestedCompound.putByte("b", (byte)(2));
		expected.put("compound", nestedCompound);
		assertSuccess(expected,
			"""
			nbtCompound (
				byte : nbtByte ( 42Y ) ,
				short : nbtShort ( 42S ) ,
				int : nbtInt ( 42 ) ,
				long : nbtLong ( 42 ) ,
				float : nbtFloat ( 42 ) ,
				double : nbtDouble ( 42 ) ,
				string : nbtString ( '42' ) ,
				byteArray0 : nbtByteArray ( ) ,
				byteArray1 : nbtByteArray ( 42Y ) ,
				byteArray2 : nbtByteArray ( 42Y , 123Y ) ,
				intArray0 : nbtIntArray ( ) ,
				intArray1 : nbtIntArray ( 42 ) ,
				intArray2 : nbtIntArray ( 42 , 123 ) ,
				longArray0 : nbtLongArray ( ) ,
				longArray1 : nbtLongArray ( 42 ) ,
				longArray2 : nbtLongArray ( 42 , 123 ) ,
				list0 : nbtList ( ) ,
				list1 : nbtList ( nbtByte ( 42Y ) ),
				list2 : nbtList ( nbtByte ( 42Y ), nbtByte ( 123Y ) ),
				compound : nbtCompound (
					a : nbtByte ( 1Y ) ,
					b : nbtByte ( 2Y )
				)
			)
			"""
		);
	}

	@Test
	public void testCasting() throws ScriptParsingException {
		assertSuccess((byte)(42), "nbtByte ( 42Y ) . asByte ( )");
		assertSuccess((short)(42), "nbtShort ( 42S ) . asShort ( )");
		assertSuccess((int)(42), "nbtInt ( 42 ) . asInt ( )");
		assertSuccess((long)(42), "nbtLong ( 42 ) . asLong ( )");
		assertSuccess((float)(42), "nbtFloat ( 42 ) . asFloat ( )");
		assertSuccess((double)(42), "nbtDouble ( 42 ) . asDouble ( )");
		assertSuccess("42", "nbtString ( '42' ) . asString ( )");
		assertSuccess((byte)(0), "NbtByte ( null ) . asByte ( )");
	}

	@Test
	public void testMembers() throws ScriptParsingException {
		assertSuccess(1, "nbtCompound ( a : nbtByte ( 1Y ) , b : nbtByte ( 2Y ) ) . a . asInt ( )");
		assertSuccess(null, "nbtCompound ( ) . missing");
		assertSuccess(null, "nbtByte ( 0Y ) . notACompound");
		assertSuccess(null, "nbtByteArray ( 1Y , 2Y ) . ( -1 )");
		assertSuccess((byte)(1), "nbtByteArray ( 1Y , 2Y ) . ( 0 ) . asByte ( )");
		assertSuccess((byte)(2), "nbtByteArray ( 1Y , 2Y ) . ( 1 ) . asByte ( )");
		assertSuccess(null, "nbtByteArray ( 1Y , 2Y ) . ( 2 )");
	}

	@Test
	public void testMemberAssignment() throws ScriptParsingException {
		assertSuccess(
			compound(c -> c.putByte("a", (byte)(1))),
			"""
			var c = nbtCompound ( )
			c . a = nbtByte ( 1Y )
			c
			"""
		);
		assertSuccess(ByteTag.valueOf((byte)(1)),
			"""
			Nbt nbt = nbtCompound()
			nbt.a = 1Y
			nbt.a
			"""
		);
		assertSuccess(ByteTag.valueOf((byte)(1)),
			"""
			Nbt nbt = nbtCompound()
			nbt.('a') = 1Y
			nbt.a
			"""
		);
	}

	public static ListTag listOf(Tag... elements) {
		ListTag list = new ListTag();
		list.addAll(Arrays.asList(elements));
		return list;
	}

	public static CompoundTag compound(Consumer<CompoundTag> initializer) {
		CompoundTag compound = new CompoundTag();
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
		return new ScriptParser<>(ObjectSupplier.class, script).addEnvironment(MathScriptEnvironment.INSTANCE).configureEnvironment(NbtScriptEnvironment.createMutable()).parse(new ScriptClassLoader()).getAsObject();
	}
}