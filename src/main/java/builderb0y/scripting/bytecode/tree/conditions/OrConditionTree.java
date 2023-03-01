package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;

public class OrConditionTree implements ConditionTree {

	public final ConditionTree left, right;

	public OrConditionTree(ConditionTree left, ConditionTree right) {
		this.left = left;
		this.right = right;
	}

	public static ConditionTree create(ConditionTree left, ConditionTree right) {
		return new OrConditionTree(left, right);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		boolean madeTrue = ifTrue == null;
		if (madeTrue) ifTrue = new Label();
		this.left.emitBytecode(method, ifTrue, null);
		//context.node.visitLabel(new Label());
		this.right.emitBytecode(method, ifTrue, null);
		//context.node.visitLabel(new Label());
		if (ifFalse != null) method.node.visitJumpInsn(Opcodes.GOTO, ifFalse);
		if (madeTrue) method.node.visitLabel(ifTrue);
	}
}