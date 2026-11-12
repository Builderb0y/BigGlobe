package builderb0y.scripting.optimization;

import java.util.Collections;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.*;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
inlines constants stored in local variables.
for example: {@code
	int x = 0
	print(x)
}
this optimizer will replace that with print(0) instead.
*/
public class InlineVariableOptimizer implements MethodOptimizer {

	public static final InlineVariableOptimizer INSTANCE = new InlineVariableOptimizer();

	@Override
	public boolean optimize(ClassNode clazz, MethodNode method) {
		Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
		Frame<SourceValue>[] analysis;
		try {
			analysis = analyzer.analyzeAndComputeMaxs(clazz.name, method);
		}
		catch (AnalyzerException exception) {
			BigGlobeMod.LOGGER.error("Exception analyzing " + clazz.name + "." + method.name + method.desc + ". This is probably really bad.", exception);
			return false;
		}
		int index = 0;
		boolean changed = false;
		for (AbstractInsnNode node = method.instructions.getFirst(); node != null;) {
			AbstractInsnNode next = node.getNext();
			switch (node.getOpcode()) {
				case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
					SourceValue sources = analysis[index].getLocal(((VarInsnNode)(node)).var);
					if (sources.insns.size() == 1) {
						AbstractInsnNode replacement = sources.insns.iterator().next().getPrevious();
						switch (replacement.getOpcode()) {
							case
								ACONST_NULL,
								ICONST_M1,
								ICONST_0,
								ICONST_1,
								ICONST_2,
								ICONST_3,
								ICONST_4,
								ICONST_5,
								LCONST_0,
								LCONST_1,
								FCONST_0,
								FCONST_1,
								FCONST_2,
								DCONST_0,
								DCONST_1,
								BIPUSH,
								SIPUSH,
								LDC
							-> {
								method.instructions.set(node, replacement.clone(Collections.emptyMap()));
								changed = true;
							}
						}
					}
				}
			}
			node = next;
			index++;
		}
		return changed;
	}
}