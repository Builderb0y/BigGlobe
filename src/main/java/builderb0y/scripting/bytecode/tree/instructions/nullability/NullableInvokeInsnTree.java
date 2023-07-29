package builderb0y.scripting.bytecode.tree.instructions.nullability;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.InvokeInstanceInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableInvokeInsnTree extends InvokeInstanceInsnTree {

	public NullableInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		for (InsnTree arg : this.args) {
			arg.emitBytecode(method);
		}
		this.method.emit(method, this.opcode());
		method.node.visitInsn(this.method.returnType.isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.method.returnType, end);
		method.node.visitInsn(this.method.returnType.isDoubleWidth() ? POP2 : POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);

		method.node.visitLabel(end);
	}
}