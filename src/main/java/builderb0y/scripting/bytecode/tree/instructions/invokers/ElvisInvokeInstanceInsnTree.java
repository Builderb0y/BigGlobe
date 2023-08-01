package builderb0y.scripting.bytecode.tree.instructions.invokers;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ElvisInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisInvokeInstanceInsnTree extends InvokeBaseInsnTree {

	public InsnTree receiver;
	public InsnTree runtimeMethodCaster;
	public InsnTree compileAlternative, runtimeAlternative;

	public ElvisInvokeInstanceInsnTree(
		InsnTree receiver,
		MethodInfo method,
		InsnTree runtimeMethodCaster,
		InsnTree compileAlternative,
		InsnTree runtimeAlternative,
		InsnTree... args
	) {
		super(method, args);
		checkArguments(method.paramTypes, args);
		InvokeInstanceInsnTree.checkReceiver(method.owner, receiver);
		if (!runtimeMethodCaster.getTypeInfo().equals(runtimeAlternative.getTypeInfo())) {
			throw new IllegalArgumentException("Type mismatch between runtimeMethodCaster (" + runtimeMethodCaster.getTypeInfo() + ") and runtimeAlternative (" + runtimeAlternative.getTypeInfo() + ')');
		}
		this.receiver = receiver;
		this.runtimeMethodCaster = runtimeMethodCaster;
		this.compileAlternative = compileAlternative;
		this.runtimeAlternative = runtimeAlternative;
	}

	public static ElvisInvokeInstanceInsnTree create(ExpressionParser parser, InsnTree receiver, MethodInfo method, InsnTree alternative, InsnTree... args) {
		IfElseInsnTree.Operands operands = IfElseInsnTree.Operands.of(parser, getFromStack(method.returnType), alternative);
		return new ElvisInvokeInstanceInsnTree(receiver, method, operands.runtimeTrue(), operands.compileFalse(), operands.runtimeFalse(), args);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), alternative = label(), end = label();

		this.receiver.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitJumpInsn(IFNONNULL, get);
		method.node.visitInsn(POP);
		method.node.visitJumpInsn(GOTO, alternative);

		method.node.visitLabel(get);
		this.method.emit(method);
		this.runtimeMethodCaster.emitBytecode(method);
		method.node.visitInsn(this.method.returnType.isDoubleWidth() ? DUP2 : DUP);
		ElvisInsnTree.jumpIfNonNull(method, this.method.returnType, end);
		method.node.visitInsn(this.method.returnType.isDoubleWidth() ? POP2 : POP);

		method.node.visitLabel(alternative);
		this.runtimeAlternative.emitBytecode(method);

		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.runtimeAlternative.getTypeInfo();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree methodCaster = getFromStack(this.method.returnType).cast(parser, type, mode);
		if (methodCaster == null) return null;
		InsnTree alternative = this.compileAlternative.cast(parser, type, mode);
		if (alternative == null) return null;
		return new ElvisInvokeInstanceInsnTree(this.receiver, this.method, methodCaster, alternative, alternative, this.args);
	}
}