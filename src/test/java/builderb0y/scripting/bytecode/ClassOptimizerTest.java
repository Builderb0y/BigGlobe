package builderb0y.scripting.bytecode;

import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import builderb0y.scripting.TestCommon;
import builderb0y.scripting.optimization.ClassOptimizer;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static org.junit.jupiter.api.Assertions.*;

/**
results: converting a ClassNode to a byte[] flattens all the labels automatically,
which is super helpful cause now I don't have to do that myself.
*/
public class ClassOptimizerTest extends TestCommon {

	@Test
	@Disabled
	public void testUnreachable() throws ScriptParsingException {
		dumpBytecode(
			"""
			block (
				block (
					block (
						return('a')
					)
					return('b')
				)
				return('c')
			)
			return('d')
			"""
		);
	}

	@Test
	@Disabled
	public void testDoubleJump() throws ScriptParsingException {
		dumpBytecode(
			"""
			block (
				block (
					block (
						break()
					)
					break()
				)
				break()
			)
			return(null)
			"""
		);
	}

	@Test
	@Disabled
	void testEmptyInfiniteLoop() throws ScriptParsingException {
		try {
			dumpBytecode(
				"""
				block (
					block (
						block (
							break()
						)
						break()
					)
					continue()
				)
				return(null)
				"""
			);
			fail();
		}
		catch (IllegalStateException expected) {}
	}

	@Test
	@Disabled
	public void testJumpToReturn() throws ScriptParsingException {
		dumpBytecode(
			"""
			boolean condition = true
			return(condition ? 'a' : 'b')
			"""
		);
	}

	@Test
	@Disabled
	public void testConstantJumpBoolean() throws ScriptParsingException {
		dumpBytecode(
			"""
			boolean condition = true
			if (condition ? false : true:
				print('never!')
			)
			return(null)
			"""
		);
	}

	@Test
	@Disabled
	public void testPop() throws ScriptParsingException {
		dumpBytecode(
			"""
			class A(int x)
			A a = null
			return(int(a.?x ?: 0))
			"""
		);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void dumpBytecode(String script) throws ScriptParsingException {
		ScriptParser<Supplier<Object>> parser = new ScriptParser<>((Class)(Supplier.class), script);
		parser.toBytecode();
		System.out.println("################################################################");
		System.out.println("INITIAL:");
		System.out.println("################################################################");
		System.out.println(parser.clazz.dump());

		ClassOptimizer.DEFAULT.optimize(parser.clazz.node);
		System.out.println("################################################################");
		System.out.println("AFTER OPTIMIZING:");
		System.out.println("################################################################");
		System.out.println(parser.clazz.dump());

		ClassNode newNode = new ClassNode();
		new ClassReader(parser.clazz.toByteArray()).accept(newNode, 0);
		parser.clazz.node = newNode;
		System.out.println("################################################################");
		System.out.println("AFTER SERIALIZING:");
		System.out.println("################################################################");
		System.out.println(parser.clazz.dump());
	}
}