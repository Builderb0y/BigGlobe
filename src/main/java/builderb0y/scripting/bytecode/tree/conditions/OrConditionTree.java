package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OrConditionTree implements ConditionTree {

	public final ConditionTree left, right;

	public OrConditionTree(ConditionTree left, ConditionTree right) {
		this.left = left;
		this.right = right;
	}

	public static ConditionTree create(ConditionTree left, ConditionTree right) {
		if (left instanceof ConstantConditionTree leftConstant) {
			if (right instanceof ConstantConditionTree rightConstant) {
				return ConstantConditionTree.of(leftConstant.value | rightConstant.value);
			}
			else {
				return leftConstant.value ? leftConstant : right;
			}
		}
		else {
			//don't short-circuit if the right condition is constant.
			//left still needs to be evaluated either way.
			return new OrConditionTree(left, right);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		boolean madeTrue = ifTrue == null;
		if (madeTrue) ifTrue = label();
		this.left.emitBytecode(method, ifTrue, null);
		//context.node.visitLabel(new Label());
		this.right.emitBytecode(method, ifTrue, null);
		//context.node.visitLabel(new Label());
		if (ifFalse != null) method.node.visitJumpInsn(Opcodes.GOTO, ifFalse);
		if (madeTrue) method.node.visitLabel(ifTrue);
	}
}