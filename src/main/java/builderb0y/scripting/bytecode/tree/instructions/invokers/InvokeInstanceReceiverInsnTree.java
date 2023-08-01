package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeInstanceReceiverInsnTree extends InvokeBaseInsnTree {

	public InsnTree receiver;

	public InvokeInstanceReceiverInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(method, args);
		this.receiver = receiver;
		checkArguments(method.paramTypes, args);
		InvokeInstanceInsnTree.checkReceiver(method.owner, receiver);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.receiver.emitBytecode(method);
		method.node.visitInsn(this.receiver.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		super.emitBytecode(method);
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.receiver.getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return new InvokeInstanceInsnTree(this.receiver, this.method, this.args).asStatement();
	}
}