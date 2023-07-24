package builderb0y.scripting.optimization;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
optimizes jump instructions which don't actually skip over anything.
in other words, they jump to a label which either follows the jump,
or a label which follows one or more other labels which follow the jump.
in the case of {@link Opcodes#GOTO}, the jump can simply be removed.
in the case of IF... or IF_ICMP... or IF_ACMP..., the operands to the
instruction need to be popped, but the jump itself can be removed.
effectively, in this case the jump is replaced with {@link Opcodes#POP}
or {@link Opcodes#POP2}, depending on how many operands are on the stack.

example code to generate a GOTO for the next instruction: {@code
	block (
		break()
	)
}
example code to generate an IF_ICMPEQ for the next instruction: {@code
	block (
		int a = int b := 1
		if (a == b: break())
	)
}
*/
public class JumpToNextInstructionOptimizer implements MethodOptimizer {

	public static final JumpToNextInstructionOptimizer INSTANCE = new JumpToNextInstructionOptimizer();

	@Override
	public boolean optimize(MethodNode method) {
		boolean changedAny = false;
		for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;) {
			AbstractInsnNode next = instruction.getNext();
			if (instruction instanceof JumpInsnNode jump) {
				if (isJumpToNextInstruction(jump)) {
					switch (jump.getOpcode()) {
						case GOTO -> method.instructions.remove(jump);
						case JSR, RET -> throw new UnsupportedOperationException("JSR/RET not supported.");
						case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL -> method.instructions.set(jump, new InsnNode(POP));
						case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> method.instructions.set(jump, new InsnNode(POP2));
						default -> throw new IllegalStateException("Non-jump opcode for JumpInsnNode: " + jump.getOpcode());
					}
					changedAny = true;
				}
			}
			instruction = next;
		}
		return changedAny;
	}

	public static boolean isJumpToNextInstruction(JumpInsnNode jump) {
		AbstractInsnNode node = jump.label;
		while (true) {
			if (node == jump) return true;
			else if (node instanceof LabelNode || node instanceof LineNumberNode) node = node.getPrevious();
			else return false;
		}
	}
}