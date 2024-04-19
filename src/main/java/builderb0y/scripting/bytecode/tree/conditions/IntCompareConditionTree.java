package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class IntCompareConditionTree implements ConditionTree {

	public final InsnTree left, right;
	public final int opcode;

	public IntCompareConditionTree(InsnTree left, InsnTree right, int opcode) {
		this.left = left;
		this.right = right;
		this.opcode = opcode;
	}

	@FunctionalInterface
	public static interface IntBiPredicate {

		public abstract boolean test(int left, int right);
	}

	public static ConditionTree createInt(InsnTree left, InsnTree right, int normalOpcode, int zeroOpcode, IntBiPredicate ifConstant) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant()) {
			if (rightConstant.isConstant()) {
				return ConstantConditionTree.of(ifConstant.test(leftConstant.asInt(), rightConstant.asInt()));
			}
			else {
				if (leftConstant.asInt() == 0) {
					return new IntCompareZeroConditionTree(right, ConditionTree.flipOpcode(zeroOpcode));
				}
			}
		}
		else {
			if (rightConstant.isConstant()) {
				if (rightConstant.asInt() == 0) {
					return new IntCompareZeroConditionTree(left, zeroOpcode);
				}
			}
		}
		return new IntCompareConditionTree(left, right, normalOpcode);
	}

	public static ConditionTree equal(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPEQ, IFEQ, (int l, int r) -> l == r);
	}

	public static ConditionTree notEqual(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPNE, IFNE, (int l, int r) -> l != r);
	}

	public static ConditionTree lessThan(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPLT, IFLT, (int l, int r) -> l < r);
	}

	public static ConditionTree greaterThan(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPGT, IFGT, (int l, int r) -> l > r);
	}

	public static ConditionTree lessThanOrEqual(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPLE, IFLE, (int l, int r) -> l <= r);
	}

	public static ConditionTree greaterThanOrEqual(InsnTree left, InsnTree right) {
		return createInt(left, right, IF_ICMPGE, IFGE, (int l, int r) -> l >= r);
	}

	public void toInt(MethodCompileContext method) {}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		this.toInt(method);
		if (ifTrue != null) {
			method.node.visitJumpInsn(this.opcode, ifTrue);
			if (ifFalse != null) {
				method.node.visitJumpInsn(GOTO, ifFalse);
			}
		}
		else {
			method.node.visitJumpInsn(ConditionTree.negateOpcode(this.opcode), ifFalse);
		}
	}
}