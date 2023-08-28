package builderb0y.scripting.bytecode.tree.flow.compare;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class LongCompareInsnTree extends IntLikeCompareInsnTree {

	public LongCompareInsnTree(
		InsnTree left,
		InsnTree right,
		InsnTree lessThan,
		InsnTree equalTo,
		InsnTree greaterThan,
		TypeInfo type
	) {
		super(left, right, lessThan, equalTo, greaterThan, type);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();

		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitInsn(LCMP);
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

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree lessThan = this.lessThan.cast(parser, type, mode);
		if (lessThan == null) return null;
		InsnTree equalTo = this.equalTo.cast(parser, type, mode);
		if (equalTo == null) return null;
		InsnTree greaterThan = this.greaterThan.cast(parser, type, mode);
		if (greaterThan == null) return null;
		return new LongCompareInsnTree(this.left, this.right, lessThan, equalTo, greaterThan, type);
	}
}