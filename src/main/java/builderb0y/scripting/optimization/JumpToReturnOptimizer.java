package builderb0y.scripting.optimization;

import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
optimizes cases where a GOTO instruction jumps to a return instruction.
in these cases, the return is inlined into the goto's location.

example code which generates such a case: {@code
	return(condition ? a : b)
}
*/
public class JumpToReturnOptimizer implements MethodOptimizer {

	public static final JumpToReturnOptimizer INSTANCE = new JumpToReturnOptimizer();

	@Override
	public boolean optimize(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode node = method.instructions.getFirst(); node != null;) {
			AbstractInsnNode next = node.getNext();
			if (node.getOpcode() == GOTO) {
				AbstractInsnNode target = ((JumpInsnNode)(node)).label;
				do target = target.getNext();
				while (target instanceof LabelNode || target instanceof LineNumberNode);
				if (target != null) switch (target.getOpcode()) {
					case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> {
						method.instructions.set(node, new InsnNode(target.getOpcode()));
						changed = true;
					}
					default -> {}
				}
			}
			node = next;
		}
		return changed;
	}
}