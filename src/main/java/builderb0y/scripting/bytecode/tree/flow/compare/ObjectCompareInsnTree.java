package builderb0y.scripting.bytecode.tree.flow.compare;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ObjectCompareInsnTree extends FloatLikeCompareInsnTree {

	public static final MethodInfo COMPARE = MethodInfo.getMethod(ObjectCompareInsnTree.class, "compare");

	public ObjectCompareInsnTree(
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
		COMPARE.emit(method, INVOKESTATIC);
		method.node.visitInsn(DUP);

		LabelNode
			greaterIncomparable = labelNode(),
			greaterThan         = labelNode(),
			lessThan            = labelNode(),
			equalTo             = labelNode(),
			incomparable        = labelNode(),
			end                 = labelNode();

		method.node.visitJumpInsn(IFGT, greaterIncomparable.getLabel());
		//less than or equal.
		method.node.visitJumpInsn(IFLT, lessThan.getLabel());
		method.node.visitJumpInsn(GOTO, equalTo.getLabel());
		method.node.visitLabel(greaterIncomparable.getLabel());
		//greater than or incomparable.
		method.node.visitInsn(ICONST_1);
		method.node.visitJumpInsn(IF_ICMPGT, incomparable.getLabel());
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

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree lessThan = this.lessThan.cast(parser, type, mode);
		if (lessThan == null) return null;
		InsnTree equalTo = this.equalTo.cast(parser, type, mode);
		if (equalTo == null) return null;
		InsnTree greaterThan = this.greaterThan.cast(parser, type, mode);
		if (greaterThan == null) return null;
		return new ObjectCompareInsnTree(this.left, this.right, lessThan, equalTo, greaterThan, this.incomparable, type);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compare(Comparable a, Comparable b) {
		return a == null || b == null ? 2 : Integer.signum(a.compareTo(b));
	}
}