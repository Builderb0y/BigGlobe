package builderb0y.scripting.bytecode.tree.flow.compare;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class IntCompareZeroInsnTree extends IntLikeCompareInsnTree {

	public IntCompareZeroInsnTree(
		InsnTree value,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		TypeInfo type
	) {
		super(value, ldc(0), lessThan, equalTo, greaterThan, type);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();
		this.left.emitBytecode(method);
		method.node.visitInsn(DUP);

		LabelNode
			greaterThan = labelNode(),
			lessThan    = labelNode(),
			equalTo     = labelNode(),
			end         = labelNode();
		method.node.visitJumpInsn(IFGT, greaterThan.getLabel());
		method.node.visitJumpInsn(IFLT, lessThan.getLabel());
		method.node.visitJumpInsn(GOTO, equalTo.getLabel());

		Scope scope = method.scopes.pushManualScope();

		scope.start = greaterThan;
		scope.end = lessThan;
		method.node.visitLabel(greaterThan.getLabel());
		method.node.visitInsn(POP);
		this.greaterThan.emitBytecode(method);
		if (!this.greaterThan.jumpsUnconditionally()) {
			method.node.visitJumpInsn(GOTO, end.getLabel());
		}

		scope.start = lessThan;
		scope.end = equalTo;
		method.node.visitLabel(lessThan.getLabel());
		this.lessThan.emitBytecode(method);
		if (!this.lessThan.jumpsUnconditionally()) {
			method.node.visitJumpInsn(GOTO, end.getLabel());
		}

		scope.start = equalTo;
		scope.end = end;
		method.node.visitLabel(equalTo.getLabel());
		this.equalTo.emitBytecode(method);
		if (!this.equalTo.jumpsUnconditionally()) {
			method.node.visitJumpInsn(GOTO, end.getLabel());
		}

		method.node.visitLabel(end.getLabel());

		method.scopes.popManualScope();
		method.scopes.popScope();
	}
}