package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableInvokeInstanceReceiverInsnTree extends InvokeBaseInsnTree {

	public InsnTree receiver;

	public NullableInvokeInstanceReceiverInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(method, args);
		this.receiver = receiver;
		checkArguments(method.paramTypes, args);
		InvokeInstanceInsnTree.checkReceiver(method.owner, receiver);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label invoke = label(), end = label();

		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, invoke);
		method.node.visitInsn(POP);
		method.node.visitInsn(ACONST_NULL);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(invoke);
		method.node.visitInsn(DUP);
		super.emitBytecode(method);
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.receiver.getTypeInfo();
	}
}