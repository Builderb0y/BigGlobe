package builderb0y.scripting.bytecode.tree.instructions.unary;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;
import static org.junit.jupiter.api.Assertions.*;

public class CastInsnTreeTest {

	public static final int[]
		I2Z = {       IFNE, ICONST_0, GOTO, -1, ICONST_1, -1 },
		L2Z = { LCMP, IFNE, ICONST_0, GOTO, -1, ICONST_1, -1 };

	@Test
	public void testPrimitives() {
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.BOOLEAN);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.BYTE);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.CHAR);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.SHORT);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.INT);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.LONG, I2L);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.FLOAT, I2F);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BOOLEAN, TypeInfos.DOUBLE, I2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.BYTE, TypeInfos.BOOLEAN, I2Z);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.BYTE);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.CHAR);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.SHORT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.INT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.LONG, I2L);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.FLOAT, I2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.BYTE, TypeInfos.DOUBLE, I2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.CHAR, TypeInfos.BOOLEAN, I2Z);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.CHAR, TypeInfos.BYTE, I2B);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.CHAR);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.SHORT, I2S);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.INT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.LONG, I2L);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.FLOAT, I2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.CHAR, TypeInfos.DOUBLE, I2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.SHORT, TypeInfos.BOOLEAN, I2Z);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.SHORT, TypeInfos.BYTE, I2B);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.SHORT, TypeInfos.CHAR, I2C);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.SHORT, TypeInfos.SHORT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.SHORT, TypeInfos.INT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.SHORT, TypeInfos.LONG, I2L);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.SHORT, TypeInfos.FLOAT, I2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.SHORT, TypeInfos.DOUBLE, I2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT, TypeInfos.BOOLEAN, I2Z);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT, TypeInfos.BYTE, I2B);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT, TypeInfos.CHAR, I2C);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT, TypeInfos.SHORT, I2S);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.INT, TypeInfos.INT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.INT, TypeInfos.LONG, I2L);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.INT, TypeInfos.FLOAT, I2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.INT, TypeInfos.DOUBLE, I2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.LONG, TypeInfos.BOOLEAN, L2Z);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.LONG, TypeInfos.BYTE, L2I, I2B);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.LONG, TypeInfos.CHAR, L2I, I2C);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.LONG, TypeInfos.SHORT, L2I, I2S);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.LONG, TypeInfos.INT, L2I);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.LONG, TypeInfos.LONG);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.LONG, TypeInfos.FLOAT, L2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.LONG, TypeInfos.DOUBLE, L2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.BOOLEAN, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.BYTE, INVOKESTATIC, I2B);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.CHAR, INVOKESTATIC, I2C);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.SHORT, INVOKESTATIC, I2S);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.INT, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.LONG, INVOKESTATIC);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.FLOAT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.FLOAT, TypeInfos.DOUBLE, F2D);

		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.BOOLEAN, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.BYTE, INVOKESTATIC, I2B);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.CHAR, INVOKESTATIC, I2C);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.SHORT, INVOKESTATIC, I2S);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.INT, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.LONG, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.FLOAT, D2F);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.DOUBLE, TypeInfos.DOUBLE);
	}

	public static ClassCompileContext clazz() {
		return new ClassCompileContext(
			ACC_PUBLIC,
			ClassType.CLASS,
			"test",
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
	}

	public void test(CastMode mode, TypeInfo from, TypeInfo to, int... expectedOpcodes) {
		ClassCompileContext clazz = clazz();
		MethodCompileContext method = clazz.newMethod(ACC_PUBLIC, "test", TypeInfos.VOID);
		ExpressionParser parser = new ExpressionParser("", clazz, method);
		load("x", 0, from).cast(parser, to, mode).emitBytecode(method);
		this.checkInstructions(method.node, expectedOpcodes);
		if (!mode.implicit) {
			this.assertFail(mode.toImplicit(), from, to);
		}
	}

	public void assertFail(CastMode mode, TypeInfo from, TypeInfo to) {
		try {
			ClassCompileContext clazz = clazz();
			MethodCompileContext method = clazz.newMethod(ACC_PUBLIC, "test", TypeInfos.VOID);
			ExpressionParser parser = new ExpressionParser("", clazz, method);
			load("x", 0, from).cast(parser, to, mode);
			fail();
		}
		catch (ClassCastException expected) {}
	}

	public void checkInstructions(MethodNode node, int... expectedOpcodes) {
		AbstractInsnNode[] instructions = node.instructions.toArray();
		if (instructions.length != expectedOpcodes.length + 1) {
			fail("Wrong number of instructions");
		}
		for (int index = 0, length = expectedOpcodes.length; index < length; index++) {
			if (instructions[index + 1].getOpcode() != expectedOpcodes[index]) {
				fail("Wrong opcode at index " + index);
			}
		}
	}
}