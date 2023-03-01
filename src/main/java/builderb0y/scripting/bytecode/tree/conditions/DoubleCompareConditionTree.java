package builderb0y.scripting.bytecode.tree.conditions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class DoubleCompareConditionTree extends IntCompareConditionTree {

	public final int toIntOpcode;

	public DoubleCompareConditionTree(InsnTree left, InsnTree right, int opcode, int toIntOpcode) {
		super(left, right, opcode);
		this.toIntOpcode = toIntOpcode;
	}

	@FunctionalInterface
	public static interface DoubleBiPredicate {

		public abstract boolean test(double l, double r);
	}

	public static ConditionTree createDouble(InsnTree left, InsnTree right, int opcode, int toIntOpcode, DoubleBiPredicate ifConstant) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return new ConstantConditionTree(ifConstant.test(leftConstant.asDouble(), rightConstant.asDouble()));
		}
		else {
			return new DoubleCompareConditionTree(left, right, opcode, toIntOpcode);
		}
	}

	public static ConditionTree equal(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFEQ, DCMPL, (l, r) -> l == r);
	}

	public static ConditionTree notEqual(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFNE, DCMPL, (l, r) -> l != r);
	}

	public static ConditionTree lessThan(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFLT, DCMPG, (l, r) -> l < r);
	}

	public static ConditionTree greaterThan(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFGT, DCMPL, (l, r) -> l > r);
	}

	public static ConditionTree lessThanOrEqual(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFLE, DCMPG, (l, r) -> l <= r);
	}

	public static ConditionTree greaterThanOrEqual(InsnTree left, InsnTree right) {
		return createDouble(left, right, IFGE, DCMPL, (l, r) -> l >= r);
	}

	@Override
	public void toInt(MethodCompileContext method) {
		method.node.visitInsn(this.toIntOpcode);
	}
}