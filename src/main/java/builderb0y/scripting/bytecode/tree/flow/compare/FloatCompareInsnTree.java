package builderb0y.scripting.bytecode.tree.flow.compare;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class FloatCompareInsnTree extends FloatLikeCompareInsnTree {

	public FloatCompareInsnTree(
		InsnTree left,
		InsnTree right,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		InsnTree incomparable,
		TypeInfo type
	) {
		super(left, right, lessThan, equalTo, greaterThan, incomparable, type);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitInsn(DUP2);

		LabelNode
			greaterEqual = labelNode(),
			greaterThan  = labelNode(),
			lessThan     = labelNode(),
			equalTo      = labelNode(),
			incomparable = labelNode(),
			end          = labelNode();

		method.node.visitInsn(FCMPL);
		method.node.visitJumpInsn(IFGE, greaterEqual.getLabel());
		//less than or incomparable.
		method.node.visitInsn(FCMPG);
		method.node.visitJumpInsn(IFLT, lessThan.getLabel());
		method.node.visitJumpInsn(GOTO, incomparable.getLabel());
		method.node.visitLabel(greaterEqual.getLabel());
		//greater than or equal.
		method.node.visitInsn(FCMPL);
		method.node.visitJumpInsn(IFEQ, equalTo.getLabel());
		method.node.visitJumpInsn(GOTO, greaterThan.getLabel());

		Scope scope = method.scopes.pushManualScope();

		scope.start = greaterThan;
		scope.end = lessThan;
		method.node.visitLabel(greaterThan.getLabel());
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
		scope.end = incomparable;
		method.node.visitLabel(equalTo.getLabel());
		this.equalTo.emitBytecode(method);
		if (!this.equalTo.jumpsUnconditionally()) {
			method.node.visitJumpInsn(GOTO, end.getLabel());
		}

		scope.start = incomparable;
		scope.end = end;
		method.node.visitLabel(incomparable.getLabel());
		this.incomparable.emitBytecode(method);
		if (!this.equalTo.jumpsUnconditionally()) {
			method.node.visitJumpInsn(GOTO, end.getLabel());
		}

		method.node.visitLabel(end.getLabel());

		method.scopes.popManualScope();
		method.scopes.popScope();
	}
}