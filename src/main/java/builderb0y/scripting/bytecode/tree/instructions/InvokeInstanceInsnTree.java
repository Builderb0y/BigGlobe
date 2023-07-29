package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeInstanceInsnTree extends InvokeBaseInsnTree {

	public InsnTree receiver;

	public InvokeInstanceInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(method, args);
		this.receiver = receiver;
		checkReceiver(method.owner, receiver);
	}

	public static void checkReceiver(TypeInfo requirement, InsnTree receiver) {
		if (!receiver.getTypeInfo().extendsOrImplements(requirement)) {
			throw new IllegalArgumentException("Receiver is of the wrong type! Expected " + requirement + ", got " + receiver.describe());
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		super.emitBytecode(method);
	}
}