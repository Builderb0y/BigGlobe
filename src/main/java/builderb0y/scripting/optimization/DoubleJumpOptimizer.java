package builderb0y.scripting.optimization;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

/**
optimizes cases where a jump instruction jumps to a label which is immediately
followed by a {@link Opcodes#GOTO} instruction, or is immediately followed by
one or more other labels which themselves are followed by a {@link Opcodes#GOTO} instruction.
in this case, the first jump's target label can be replaced with the GOTO's target label.

example code which generates such a case: {@code
	block (
		block (
			break()
		)
		break()
	)
}
*/
public class DoubleJumpOptimizer implements MethodOptimizer {

	public static final DoubleJumpOptimizer INSTANCE = new DoubleJumpOptimizer();

	@Override
	public boolean optimize(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
			if (instruction instanceof JumpInsnNode jump) {
				LabelNode oldJump = jump.label;
				LabelNode newJump = getDoubleJumpTarget(oldJump);
				if (oldJump != newJump) {
					jump.label = newJump;
					changed = true;
				}
			}
		}
		return changed;
	}

	public static LabelNode getDoubleJumpTarget(LabelNode start) {
		Set<LabelNode> starts = new HashSet<>(4);
		while (true) {
			if (!starts.add(start)) {
				throw new IllegalStateException("Detected empty infinite loop in script");
			}
			AbstractInsnNode target = start;
			do target = target.getNext();
			while (target instanceof LabelNode || target instanceof LineNumberNode);
			if (target != null && target.getOpcode() == Opcodes.GOTO) {
				start = ((JumpInsnNode)(target)).label;
			}
			else {
				return start;
			}
		}
	}
}