package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class BitwiseFloatEqualityConditionTree implements ConditionTree {

	public static final MethodInfo FLOAT_BITS_TO_INT = MethodInfo.getMethod(Float.class, "floatToRawIntBits");

	public InsnTree left, right;
	public int intOpCode;

	public BitwiseFloatEqualityConditionTree(InsnTree left, InsnTree right, int intOpCode) {
		if (left.getTypeInfo().getSort() != Sort.FLOAT) {
			throw new IllegalArgumentException("Left type not float: " + left.describe());
		}
		if (right.getTypeInfo().getSort() != Sort.FLOAT) {
			throw new IllegalArgumentException("Right type not float: " + right.describe());
		}
		this.left = left;
		this.right = right;
		this.intOpCode = intOpCode;
	}

	public static BitwiseFloatEqualityConditionTree equal(InsnTree left, InsnTree right) {
		return new BitwiseFloatEqualityConditionTree(left, right, IF_ICMPEQ);
	}

	public static BitwiseFloatEqualityConditionTree notEqual(InsnTree left, InsnTree right) {
		return new BitwiseFloatEqualityConditionTree(left, right, IF_ICMPNE);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.left.emitBytecode(method);
		FLOAT_BITS_TO_INT.emitBytecode(method);
		this.right.emitBytecode(method);
		FLOAT_BITS_TO_INT.emitBytecode(method);
		if (ifTrue != null) {
			method.node.visitJumpInsn(this.intOpCode, ifTrue);
			if (ifFalse != null) {
				method.node.visitJumpInsn(GOTO, ifFalse);
			}
		}
		else {
			method.node.visitJumpInsn(ConditionTree.negateOpcode(this.intOpCode), ifFalse);
		}
	}
}