package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReduceInsnTree extends InvokeStaticInsnTree {

	public ReduceInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		if (!method.isStatic() || method.paramTypes.length != 2) {
			throw new IllegalArgumentException(method.toString());
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		InsnTree[] args = this.args;
		args[0].emitBytecode(method);
		for (int index = 1, length = args.length; index < length; index++) {
			args[index].emitBytecode(method);
			this.method.emit(method, this.opcode());
		}
	}
}