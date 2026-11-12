package builderb0y.scripting.optimization;

import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

/**
removes useless line number nodes.
a line number node is considered useless if it is followed by another line number node,
or one or more label nodes which themselves are followed by another line number node.
*/
public class LineNumberOptimizer implements MethodOptimizer {

	public static final LineNumberOptimizer INSTANCE = new LineNumberOptimizer();

	@Override
	public boolean optimize(ClassNode clazz, MethodNode method) {
		boolean changedAny = false;
		LineNumberNode previous = null;
		for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
			if (node instanceof LineNumberNode lineNumberNode) {
				if (previous != null) {
					method.instructions.remove(previous);
					changedAny = true;
				}
				previous = lineNumberNode;
			}
			else if (node instanceof LabelNode) {
				//no-op
			}
			else {
				previous = null;
			}
		}
		return changedAny;
	}
}