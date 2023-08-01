package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class NullCompareConditionTree implements ConditionTree {

	public InsnTree value;
	public int opcode;

	public NullCompareConditionTree(InsnTree value, int opcode) {
		if (!value.getTypeInfo().isObject()) {
			throw new IllegalArgumentException("Value type not object: " + value.describe());
		}
		this.value = value;
		this.opcode = opcode;
	}

	public static NullCompareConditionTree isNull(InsnTree value) {
		return new NullCompareConditionTree(value, IFNULL);
	}

	public static NullCompareConditionTree nonNull(InsnTree value) {
		return new NullCompareConditionTree(value, IFNONNULL);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.value.emitBytecode(method);
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