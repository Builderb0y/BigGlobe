package builderb0y.scripting.optimization;

import java.util.BitSet;

import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
replaces "store" instructions with "pop" instructions if the referenced variable is never loaded.
this, combined with several other optimizers, should effectively remove variables that don't need to exist.
*/
public class UnnecessaryVariableOptimizer implements MethodOptimizer {

	public static final UnnecessaryVariableOptimizer INSTANCE = new UnnecessaryVariableOptimizer();

	@Override
	public boolean optimize(ClassNode clazz, MethodNode method) {
		if (method.localVariables == null) return false;
		BitSet loadedVariables = new BitSet(method.localVariables.size());
		for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
			switch (instruction.getOpcode()) {
				case ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> loadedVariables.set(((VarInsnNode)(instruction)).var);
				case IINC -> loadedVariables.set(((IincInsnNode)(instruction)).var);
			}
		}
		boolean changed = false;
		for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;) {
			AbstractInsnNode next = instruction.getNext();
			switch (instruction.getOpcode()) {
				case ISTORE, FSTORE, ASTORE -> {
					if (!loadedVariables.get(((VarInsnNode)(instruction)).var)) {
						method.instructions.set(instruction, new InsnNode(POP));
						changed = true;
					}
				}
				case LSTORE, DSTORE -> {
					if (!loadedVariables.get(((VarInsnNode)(instruction)).var)) {
						method.instructions.set(instruction, new InsnNode(POP2));
						changed = true;
					}
				}
			}
			instruction = next;
		}
		return changed;
	}
}