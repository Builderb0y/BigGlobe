package builderb0y.scripting.optimization;

import org.objectweb.asm.tree.*;

import builderb0y.scripting.optimization.ClassOptimizer.MethodOptimizer;

/**
removes local variables which are not used in their scope.
under normal circumstances, all variables will be assigned to at the beginning of their scope.
however, in rare cases, the optimizer system itself can
remove this store instruction if it can never be reached.
for example, the following code can trigger this behavior: {@code
	if (false:
		int x = 0
	)
}
even this isn't normally a problem. the real problem is even more subtle than this:
if this code (or something similar to it) appears near the end of the method,
and the method already returns unconditionally before this element is encountered
(which can also be triggered by my optimizer system),
then the scope of the local variable starts after the last instruction in the method,
and this will throw a ClassFormatError. here are two ways to trigger this bug:

option 1: {@code
	if (false:
		(
			int x = 0
		)
	)
}
option 2: {@code
	if (true: return())

	(
		int x = 0
	)
}

it would be tempting to nuke any local variable whose scope starts after the last instruction,
but an even better solution would be to nuke any local variable which is not used inside its scope.
*/
public class UnusedLocalVariableRemover implements MethodOptimizer {

	public static final UnusedLocalVariableRemover INSTANCE = new UnusedLocalVariableRemover();

	@Override
	public boolean optimize(MethodNode method) {
		return method.localVariables != null && method.localVariables.removeIf((LocalVariableNode variable) -> {
			for (AbstractInsnNode node = variable.start; node != variable.end; node = node.getNext()) {
				if (node instanceof VarInsnNode var && var.var == variable.index) {
					return false;
				}
				else if (node instanceof IincInsnNode inc && inc.var == variable.index) {
					return false;
				}
			}
			return true;
		});
	}
}