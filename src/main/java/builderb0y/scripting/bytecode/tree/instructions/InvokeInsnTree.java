package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeInsnTree extends InvokeStaticInsnTree {

	public InsnTree receiver;

	public InvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(method, args);
		this.receiver = receiver;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		super.emitBytecode(method);
	}
}