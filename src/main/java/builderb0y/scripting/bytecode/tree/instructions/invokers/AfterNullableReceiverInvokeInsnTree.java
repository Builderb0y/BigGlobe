package builderb0y.scripting.bytecode.tree.instructions.invokers;

import com.google.common.collect.ObjectArrays;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AfterNullableReceiverInvokeInsnTree extends BaseInvokeInsnTree {

	public AfterNullableReceiverInvokeInsnTree(MethodInfo method, LazyVarInfo script, InsnTree receiver, InsnTree... args) {
		super(method, AfterNullableInvokeInsnTree.concat2(load(script), receiver, args));
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public AfterNullableReceiverInvokeInsnTree(MethodInfo method, LazyVarInfo script, InsnTree... args) {
		super(method, ObjectArrays.concat(load(script), args));
		checkArguments(method.getInvokeTypes(), this.args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();

		this.emitSpecificArgs(method, 0, 2);
		method.node.visitInsn(this.args[1].getTypeInfo().isDoubleWidth() ? DUP2_X1 : DUP_X1);
		ElvisInsnTree.dupAndJumpIfNonNull(this.args[1].getTypeInfo(), get, method);
		method.node.visitInsn(POP2);
		if (this.args[1].getTypeInfo().isDoubleWidth()) {
			method.node.visitInsn(POP);
		}
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		this.emitSpecificArgs(method, 2, this.args.length);
		this.emitMethod(method);
		switch (this.method.returnType.getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree asStatement() {
		return new NullableInvokeInsnTree(this.method, this.args).asStatement();
	}
}