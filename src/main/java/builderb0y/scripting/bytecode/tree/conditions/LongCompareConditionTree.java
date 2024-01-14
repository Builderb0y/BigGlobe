package builderb0y.scripting.bytecode.tree.conditions;

import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class LongCompareConditionTree extends IntCompareConditionTree {

	public LongCompareConditionTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	@FunctionalInterface
	public static interface LongBiPredicate {

		public abstract boolean test(long l, long r);
	}

	public static ConditionTree create(InsnTree left, InsnTree right, int opcode, LongBiPredicate ifConstant) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return ConstantConditionTree.of(ifConstant.test(leftConstant.asLong(), rightConstant.asLong()));
		}
		else {
			return new LongCompareConditionTree(left, right, opcode);
		}
	}

	public static ConditionTree equal(InsnTree left, InsnTree right) {
		return create(left, right, IFEQ, (l, r) -> l == r);
	}

	public static ConditionTree notEqual(InsnTree left, InsnTree right) {
		return create(left, right, IFNE, (l, r) -> l != r);
	}

	public static ConditionTree lessThan(InsnTree left, InsnTree right) {
		return create(left, right, IFLT, (l, r) -> l < r);
	}

	public static ConditionTree greaterThan(InsnTree left, InsnTree right) {
		return create(left, right, IFGT, (l, r) -> l > r);
	}

	public static ConditionTree lessThanOrEqual(InsnTree left, InsnTree right) {
		return create(left, right, IFLE, (l, r) -> l <= r);
	}

	public static ConditionTree greaterThanOrEqual(InsnTree left, InsnTree right) {
		return create(left, right, IFGE, (l, r) -> l >= r);
	}

	@Override
	public void toInt(MethodCompileContext method) {
		method.node.visitInsn(Opcodes.LCMP);
	}
}