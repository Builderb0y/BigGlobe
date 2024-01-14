package builderb0y.scripting.bytecode.tree.conditions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class FloatCompareConditionTree extends IntCompareConditionTree {

	public final int toIntOpcode;

	public FloatCompareConditionTree(InsnTree left, InsnTree right, int opcode, int toIntOpcode) {
		super(left, right, opcode);
		this.toIntOpcode = toIntOpcode;
	}

	@FunctionalInterface
	public static interface FloatBiPredicate {

		public abstract boolean test(float l, float r);
	}

	public static ConditionTree createFloat(InsnTree left, InsnTree right, int compare, int toInt, FloatBiPredicate ifConstant) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return ConstantConditionTree.of(ifConstant.test(leftConstant.asFloat(), rightConstant.asFloat()));
		}
		else {
			return new FloatCompareConditionTree(left, right, compare, toInt);
		}
	}

	public static ConditionTree equal(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFEQ, FCMPL, (l, r) -> l == r);
	}

	public static ConditionTree notEqual(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFNE, FCMPL, (l, r) -> l != r);
	}

	public static ConditionTree lessThan(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFLT, FCMPG, (l, r) -> l < r);
	}

	public static ConditionTree greaterThan(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFGT, FCMPL, (l, r) -> l > r);
	}

	public static ConditionTree lessThanOrEqual(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFLE, FCMPG, (l, r) -> l <= r);
	}

	public static ConditionTree greaterThanOrEqual(InsnTree left, InsnTree right) {
		return createFloat(left, right, IFGE, FCMPL, (l, r) -> l >= r);
	}

	@Override
	public void toInt(MethodCompileContext method) {
		method.node.visitInsn(this.toIntOpcode);
	}
}