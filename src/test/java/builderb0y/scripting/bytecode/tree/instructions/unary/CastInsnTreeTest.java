package builderb0y.scripting.bytecode.tree.instructions.unary;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.bigglobe.features.ScriptedFeature.FeatureScript;
import builderb0y.bigglobe.scripting.ConstantFactory;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.CastingSupport.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;
import static org.junit.jupiter.api.Assertions.*;

public class CastInsnTreeTest {

	public static ClassCompileContext clazz() {
		return new ClassCompileContext(
			ACC_PUBLIC,
			ClassType.CLASS,
			"test",
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
	}

	@Test
	public void test() {
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.OBJECT,      TypeInfos.OBJECT);
		this.test(CastMode.IMPLICIT_THROW, TypeInfos.INT_WRAPPER, TypeInfos.OBJECT);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.OBJECT,      TypeInfos.INT_WRAPPER,   CHECKCAST);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT,         TypeInfos.SHORT,         I2S);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT_WRAPPER, TypeInfos.SHORT,         INVOKEVIRTUAL, I2S);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT,         TypeInfos.SHORT_WRAPPER, I2S, INVOKESTATIC);
		this.test(CastMode.EXPLICIT_THROW, TypeInfos.INT_WRAPPER, TypeInfos.SHORT_WRAPPER, INVOKEVIRTUAL, I2S, INVOKESTATIC);
	}

	public void test(CastMode mode, TypeInfo from, TypeInfo to, int... expectedOpcodes) {
		ClassCompileContext clazz = clazz();
		MethodCompileContext method = clazz.newMethod(ACC_PUBLIC, "test", TypeInfos.VOID);
		for (CasterData caster : CastingSupport.BUILTIN_CAST_PROVIDERS.search(from, to, mode)) {
			caster.caster.emitBytecode(method);
		}
		this.checkInstructions(method.node, expectedOpcodes);
		if (!mode.implicit) {
			this.assertFail(mode.toImplicit(), from, to);
		}
	}

	public void assertFail(CastMode mode, TypeInfo from, TypeInfo to) {
		try {
			CastingSupport.BUILTIN_CAST_PROVIDERS.search(from, to, mode);
			fail();
		}
		catch (ClassCastException expected) {}
	}

	public void checkInstructions(MethodNode node, int... expectedOpcodes) {
		AbstractInsnNode[] instructions = node.instructions.toArray();
		if (instructions.length != expectedOpcodes.length) {
			fail("Wrong number of instructions");
		}
		for (int index = 0, length = expectedOpcodes.length; index < length; index++) {
			if (instructions[index].getOpcode() != expectedOpcodes[index]) {
				fail("Wrong opcode at index " + index);
			}
		}
	}

	@Test
	public void testConstant() {
		this.testConstant(ldc(1), LDC);
		this.testConstant(load("test", 1, TypeInfos.INT), ILOAD, INVOKESTATIC, INVOKESTATIC);
	}

	public void testConstant(InsnTree value, int... expectedOpcodes) {
		ClassCompileContext clazz = clazz();
		MethodCompileContext method = clazz.newMethod(ACC_PUBLIC, "test", TypeInfos.VOID);
		ExpressionParser parser = new ExpressionParser("", clazz, method);
		parser.environment.castProviders = (
			new MultiCastProvider()
			.append(
				new LookupCastProvider()
				.append(TypeInfos.INT, TypeInfos.LONG, true, new ConstantCaster(new ConstantFactory(CastInsnTreeTest.class, "I2L", int.class, long.class)))
				.append(TypeInfos.LONG, TypeInfos.LONG_WRAPPER, true, new ConstantCaster(new ConstantFactory(CastInsnTreeTest.class, "wrapLong", long.class, Long.class)))
			)
		);
		value.cast(parser, TypeInfos.LONG_WRAPPER, CastMode.IMPLICIT_THROW).emitBytecode(method);
		this.checkInstructions(method.node, expectedOpcodes);
	}

	@Test
	public void testSemiConstant() {
		ClassCompileContext clazz = clazz();
		MethodCompileContext method = clazz.newMethod(ACC_PUBLIC, "test", TypeInfos.VOID);
		ExpressionParser parser = new ExpressionParser("", clazz, method);
		parser.environment.castProviders = (
			new MultiCastProvider()
			.append(
				new LookupCastProvider()
				.append(TypeInfos.INT, TypeInfos.LONG, true, new ConstantCaster(new ConstantFactory(CastInsnTreeTest.class, "I2L", int.class, long.class)))
				.append(TypeInfos.LONG, TypeInfos.LONG_WRAPPER, true, Caster.invokeStatic(method(ACC_PUBLIC | ACC_STATIC, Long.class, "valueOf", Long.class, long.class)))
			)
		);
		ldc(1).cast(parser, TypeInfos.LONG_WRAPPER, CastMode.IMPLICIT_THROW).emitBytecode(method);
		this.checkInstructions(method.node, LDC, INVOKESTATIC);
	}

	@Test
	public void testMinecraftConstants() throws ScriptParsingException {
		new FeatureScript.Holder(
			"""
			boolean condition1 = true
			boolean condition2 = false
			placeFeature(originX, originY, originZ,
				if (condition1:
					condition2
					? configuredFeature('bigglobe:overworld/caves/spider_pit')
					: 'bigglobe:overworld/caves/dungeon'
				)
				else (
					'bigglobe:overworld/trees/natural/oak'
				)
			)
			"""
		);
	}

	public static long I2L(MethodHandles.Lookup lookup, String name, Class<?> type, int value) {
		return (long)(value);
	}

	public static long I2L(int value) {
		return (long)(value);
	}

	public static Long wrapLong(MethodHandles.Lookup lookup, String name, Class<?> type, long value) {
		return Long.valueOf(value);
	}

	public static Long wrapLong(long value) {
		return Long.valueOf(value);
	}
}