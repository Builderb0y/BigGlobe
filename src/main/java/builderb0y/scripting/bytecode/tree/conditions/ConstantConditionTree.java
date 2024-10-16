package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;

public class ConstantConditionTree implements ConditionTree {

	public static final ConstantConditionTree
		TRUE  = new ConstantConditionTree(true ),
		FALSE = new ConstantConditionTree(false);

	public final boolean value;

	public ConstantConditionTree(boolean value) {
		this.value = value;
	}

	public static ConstantConditionTree of(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		if (this.value) {
			if (ifTrue != null) method.node.visitJumpInsn(Opcodes.GOTO, ifTrue);
		}
		else {
			if (ifFalse != null) method.node.visitJumpInsn(Opcodes.GOTO, ifFalse);
		}
	}
}