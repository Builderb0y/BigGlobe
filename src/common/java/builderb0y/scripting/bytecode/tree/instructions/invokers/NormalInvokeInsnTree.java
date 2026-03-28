package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class NormalInvokeInsnTree extends BaseInvokeInsnTree {

	public NormalInvokeInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public NormalInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}
}