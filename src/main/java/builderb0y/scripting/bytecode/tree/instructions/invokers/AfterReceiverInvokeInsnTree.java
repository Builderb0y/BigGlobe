package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class AfterReceiverInvokeInsnTree extends BaseInvokeInsnTree {

	public AfterReceiverInvokeInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public AfterReceiverInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitSpecificArgs(method, 0, 2);
		method.node.visitInsn(this.args[1].getTypeInfo().isDoubleWidth() ? DUP2_X1 : DUP_X1);
		this.emitSpecificArgs(method, 2, this.args.length);
		this.emitMethod(method);
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.args[1].getTypeInfo();
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