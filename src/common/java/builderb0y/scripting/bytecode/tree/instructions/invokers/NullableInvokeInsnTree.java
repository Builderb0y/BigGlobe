package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree.ElvisEmitters;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NullableInvokeInsnTree extends BaseInvokeInsnTree {

	public NullableInvokeInsnTree(InsnTree receiver, MethodInfo method, InsnTree... args) {
		super(receiver, method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public NullableInvokeInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(method.getInvokeTypes(), this.args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.emitFirstArg(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		constantAbsent(this.getTypeInfo()).emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		this.emitAllArgsExceptFirst(method);
		this.emitMethod(method);

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree elvis(ExpressionParser parser, InsnTree alternative) {
		return new ElvisGetInsnTree(ElvisEmitters.forMethod(this, alternative));
	}
}