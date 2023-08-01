package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableInvokeInsnTree extends InvokeBaseInsnTree {

	public InsnTree receiver;

	public NullableInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(method, args);
		this.receiver = receiver;
		checkArguments(method.paramTypes, args);
		InvokeInstanceInsnTree.checkReceiver(method.owner, receiver);
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
		super.emitBytecode(method);
		//method.node.visitInsn(this.method.returnType.isDoubleWidth() ? DUP2 : DUP);
		//ElvisInsnTree.jumpIfNonNull(method, this.method.returnType, end);
		//method.node.visitInsn(this.method.returnType.isDoubleWidth() ? POP2 : POP);
		//constantAbsent(this.getTypeInfo()).emitBytecode(method);

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree elvis(ExpressionParser parser, InsnTree alternative) throws ScriptParsingException {
		return ElvisInvokeInstanceInsnTree.create(parser, this.receiver, this.method, alternative, this.args);
	}
}