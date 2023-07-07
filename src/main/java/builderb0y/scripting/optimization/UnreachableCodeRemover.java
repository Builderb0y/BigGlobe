package builderb0y.scripting.optimization;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

import static org.objectweb.asm.Opcodes.*;

/**
removes instructions which are unreachable, and can never be executed.
*/
public class UnreachableCodeRemover implements MethodOptimizer {

	public static final UnreachableCodeRemover INSTANCE = new UnreachableCodeRemover();

	@Override
	public boolean optimize(MethodNode method) {
		if (method.instructions.size() == 0) return false;
		Set<AbstractInsnNode> visited = Collections.newSetFromMap(new IdentityHashMap<>(method.instructions.size()));
		collectAt(method.instructions.getFirst(), visited);
		if (visited.size() == method.instructions.size()) return false;
		for (AbstractInsnNode node = method.instructions.getFirst(); node != null;) {
			AbstractInsnNode next = node.getNext();
			if (!(node instanceof LabelNode || visited.contains(node))) {
				method.instructions.remove(node);
			}
			node = next;
		}
		return true;
	}

	public static void collectAt(AbstractInsnNode node, Set<AbstractInsnNode> visited) {
		while (node != null) {
			if (!visited.add(node)) return;
			node = switch (node.getOpcode()) {
				case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> {
					yield null;
				}
				case JSR, RET -> {
					throw new UnsupportedOperationException("JSR/RET not supported.");
				}
				case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, IFNULL, IFNONNULL -> {
					collectAt(((JumpInsnNode)(node)).label, visited);
					yield node.getNext();
				}
				case LOOKUPSWITCH -> {
					LookupSwitchInsnNode lookup = (LookupSwitchInsnNode)(node);
					for (LabelNode label : lookup.labels) {
						collectAt(label, visited);
					}
					collectAt(lookup.dflt, visited);
					yield null;
				}
				case TABLESWITCH -> {
					TableSwitchInsnNode table = (TableSwitchInsnNode)(node);
					for (LabelNode label : table.labels) {
						collectAt(label, visited);
					}
					collectAt(table.dflt, visited);
					yield null;
				}
				case GOTO -> {
					yield ((JumpInsnNode)(node)).label;
				}
				default -> {
					yield node.getNext();
				}
			};
		}
	}
}