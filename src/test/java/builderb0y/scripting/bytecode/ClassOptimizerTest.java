package builderb0y.scripting.bytecode;

import java.util.function.Supplier;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import builderb0y.scripting.TestCommon;
import builderb0y.scripting.optimization.ClassOptimizer;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;

/**
results: converting a ClassNode to a byte[] flattens all the labels automatically,
which is super helpful cause now I don't have to do that myself.
*/
public class ClassOptimizerTest extends TestCommon {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws ScriptParsingException {
		ScriptParser<Supplier<String>> parser = new ScriptParser<>(
			(Class)(Supplier.class),
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
		parser.toBytecode();
		System.out.println("INITIAL:");
		System.out.println(parser.clazz.dump());

		ClassOptimizer.DEFAULT.optimize(parser.clazz.node);
		System.out.println("AFTER OPTIMIZING:");
		System.out.println(parser.clazz.dump());

		ClassNode newNode = new ClassNode();
		new ClassReader(parser.clazz.toByteArray()).accept(newNode, 0);
		parser.clazz.node = newNode;
		System.out.println("AFTER SERIALIZING:");
		System.out.println(parser.clazz.dump());
	}
}