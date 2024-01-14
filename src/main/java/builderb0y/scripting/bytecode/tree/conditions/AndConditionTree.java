package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AndConditionTree implements ConditionTree {

	public final ConditionTree left, right;

	public AndConditionTree(ConditionTree left, ConditionTree right) {
		this.left = left;
		this.right = right;
	}

	public static ConditionTree create(ConditionTree left, ConditionTree right) {
		if (left instanceof ConstantConditionTree leftConstant) {
			if (right instanceof ConstantConditionTree rightConstant) {
				return new ConstantConditionTree(leftConstant.value & rightConstant.value);
			}
			else {
				return leftConstant.value ? right : leftConstant;
			}
		}
		else {
			if (right instanceof ConstantConditionTree rightConstant) {
				return rightConstant.value ? left : rightConstant;
			}
			else {
				return new AndConditionTree(left, right);
			}
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		boolean madeFalse = ifFalse == null;
		if (madeFalse) ifFalse = label();
		this.left.emitBytecode(method, null, ifFalse);
		//context.node.visitLabel(new Label());
		this.right.emitBytecode(method, null, ifFalse);
		//context.node.visitLabel(new Label());
		if (ifTrue != null) method.node.visitJumpInsn(Opcodes.GOTO, ifTrue);
		if (madeFalse) method.node.visitLabel(ifFalse);
	}
}