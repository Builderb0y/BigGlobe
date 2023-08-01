package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableFakeInvokeStaticInsnTree extends InvokeBaseInsnTree {

	public NullableFakeInvokeStaticInsnTree(MethodInfo method, InsnTree[] args) {
		super(method, args);
		checkArguments(method.paramTypes, args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		InsnTree[] args = this.args;
		Label get = label(), end = label();

		args[0].emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		for (int index = 1, length = args.length; index < length; index++) {
			args[index].emitBytecode(method);
		}
		this.method.emit(method, this.opcode());
		//method.node.visitInsn(this.method.returnType.isDoubleWidth() ? DUP2 : DUP);
		//ElvisInsnTree.jumpIfNonNull(method, this.method.returnType, end);
		//method.node.visitInsn(this.method.returnType.isDoubleWidth() ? POP2 : POP);
		//constantAbsent(this.getTypeInfo()).emitBytecode(method);

		method.node.visitLabel(end);
	}
}