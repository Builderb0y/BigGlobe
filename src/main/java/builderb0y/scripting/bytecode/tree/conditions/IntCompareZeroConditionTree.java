package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class IntCompareZeroConditionTree implements ConditionTree {

	public final InsnTree condition;
	public final int trueOpcode;

	public IntCompareZeroConditionTree(InsnTree condition, int trueOpcode) {
		this.condition = condition;
		this.trueOpcode = trueOpcode;
	}

	public static ConditionTree equalZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFEQ);
	}

	public static ConditionTree notEqualZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFNE);
	}

	public static ConditionTree lessThanZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFLT);
	}

	public static ConditionTree greaterThanZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFGT);
	}

	public static ConditionTree lessThanOrEqualToZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFLE);
	}

	public static ConditionTree greaterThanOrEqualToZero(InsnTree condition) {
		return new IntCompareZeroConditionTree(condition, IFGE);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.condition.emitBytecode(method);
		if (ifTrue != null) {
			method.node.visitJumpInsn(this.trueOpcode, ifTrue);
			if (ifFalse != null) {
				method.node.visitJumpInsn(Opcodes.GOTO, ifFalse);
			}
		}
		else {
			method.node.visitJumpInsn(ConditionTree.negateOpcode(this.trueOpcode), ifFalse);
		}
	}
}