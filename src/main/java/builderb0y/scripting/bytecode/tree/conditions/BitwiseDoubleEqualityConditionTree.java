package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class BitwiseDoubleEqualityConditionTree implements ConditionTree {

	public static final MethodInfo DOUBLE_BITS_TO_LONG = MethodInfo.getMethod(Double.class, "doubleToLongBits");

	public InsnTree left, right;
	public int intOpCode;

	public BitwiseDoubleEqualityConditionTree(InsnTree left, InsnTree right, int intOpCode) {
		if (left.getTypeInfo().getSort() != Sort.DOUBLE) {
			throw new IllegalArgumentException("Left type not double: " + left.describe());
		}
		if (right.getTypeInfo().getSort() != Sort.DOUBLE) {
			throw new IllegalArgumentException("Right type not double: " + right.describe());
		}
		this.left = left;
		this.right = right;
		this.intOpCode = intOpCode;
	}

	public static BitwiseDoubleEqualityConditionTree equal(InsnTree left, InsnTree right) {
		return new BitwiseDoubleEqualityConditionTree(left, right, IFEQ);
	}

	public static BitwiseDoubleEqualityConditionTree notEqual(InsnTree left, InsnTree right) {
		return new BitwiseDoubleEqualityConditionTree(left, right, IFNE);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.left.emitBytecode(method);
		DOUBLE_BITS_TO_LONG.emitBytecode(method);
		this.right.emitBytecode(method);
		DOUBLE_BITS_TO_LONG.emitBytecode(method);
		method.node.visitInsn(LCMP);
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