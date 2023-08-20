package builderb0y.scripting.optimization;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
I didn't think this was necessary until I found
	DUP
	POP
in one of my scripts while testing something unrelated.
so now it exists. in case you were wondering,
the script which contains this was: {@code
	class C(int x)
	C c = null
	c.?x ?: 0
},
and the section that generated DUP POP was in checking if c.?x was null or not.
it was first duped for the null check, and then popped
when the tree realized that ints can't be null.
*/
public class PopOptimizer implements MethodOptimizer {

	public static final PopOptimizer INSTANCE = new PopOptimizer();

	@Override
	public boolean optimize(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode node = method.instructions.getFirst(); node != null;) {
			AbstractInsnNode next = node.getNext();
			if (node.getOpcode() == POP) {
				AbstractInsnNode previous = node.getPrevious();
				if (previous != null) {
					switch (previous.getOpcode()) {
						case
							ACONST_NULL,
							ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
							FCONST_0, FCONST_1, FCONST_2,
							BIPUSH, SIPUSH,
							ILOAD, FLOAD,
							DUP
						-> {
							method.instructions.remove(previous);
							method.instructions.remove(node);
							changed = true;
						}
						case LDC -> {
							Object constant = ((LdcInsnNode)(previous)).cst;
							if (constant instanceof Integer || constant instanceof Float || constant instanceof String) {
								method.instructions.remove(previous);
								method.instructions.remove(node);
								changed = true;
							}
						}
						default -> {}
					}
				}
			}
			else if (node.getOpcode() == POP2) {
				AbstractInsnNode previous = node.getPrevious();
				if (previous != null) {
					switch (previous.getOpcode()) {
						case
							LCONST_0, LCONST_1,
							DCONST_0, DCONST_1,
							LLOAD, DLOAD,
							DUP2
						-> {
							method.instructions.remove(previous);
							method.instructions.remove(node);
							changed = true;
						}
						case LDC -> {
							Object constant = ((LdcInsnNode)(previous)).cst;
							if (constant instanceof Long || constant instanceof Double) {
								method.instructions.remove(previous);
								method.instructions.remove(node);
								changed = true;
							}
						}
						default -> {}
					}
				}
			}
			node = next;
		}
		return changed;
	}
}