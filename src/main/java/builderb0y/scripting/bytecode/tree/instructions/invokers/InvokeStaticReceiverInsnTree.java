package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class InvokeStaticReceiverInsnTree extends InvokeBaseInsnTree {

	public InvokeStaticReceiverInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(method.paramTypes, args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		InsnTree[] args = this.args;
		args[0].emitBytecode(method);
		method.node.visitInsn(args[0].getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		for (int index = 1, length = args.length; index < length; index++) {
			args[index].emitBytecode(method);
		}
		this.method.emit(method);
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
		return new InvokeStaticInsnTree(this.method, this.args).asStatement();
	}
}