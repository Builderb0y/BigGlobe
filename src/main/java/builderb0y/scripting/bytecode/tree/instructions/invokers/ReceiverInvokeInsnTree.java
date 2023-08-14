package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ReceiverInvokeInsnTree extends InvokeBaseInsnTree {

	public ReceiverInvokeInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
	}

	public ReceiverInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitFirstArg(method);
		method.node.visitInsn(this.args[0].getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		this.emitAllArgsExceptFirst(method);
		this.emitMethod(method);
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.args[0].getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return (
			(
				this.method.isStatic()
				? StaticInvokeInsnTree.create(this.method, this.args)
				: new NormalInvokeInsnTree(this.method, this.args)
			)
			.asStatement()
		);
	}
}