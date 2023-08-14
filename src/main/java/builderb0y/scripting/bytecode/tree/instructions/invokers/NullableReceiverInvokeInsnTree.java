package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableReceiverInvokeInsnTree extends InvokeBaseInsnTree {

	public NullableReceiverInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public NullableReceiverInvokeInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.emitFirstArg(method); //object
		method.node.visitInsn(DUP); //object object
		method.node.visitInsn(DUP); //object object object
		method.node.visitJumpInsn(IFNONNULL, get); //object object
		method.node.visitInsn(POP); //object
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get); //object object
		this.emitAllArgsExceptFirst(method); //object object (args)
		this.emitMethod(method); //object (return)
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		} //object

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.args[0].getTypeInfo();
	}
}