package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class IdentityEqualityConditionTree implements ConditionTree {

	public InsnTree left, right;
	public int opcode;

	public IdentityEqualityConditionTree(InsnTree left, InsnTree right, int opcode) {
		if (!left.getTypeInfo().isObject()) {
			throw new IllegalArgumentException("Left type not object: " + left.describe());
		}
		if (!right.getTypeInfo().isObject()) {
			throw new IllegalArgumentException("Right type not object: " + right.describe());
		}
		this.left = left;
		this.right = right;
		this.opcode = opcode;
	}

	public static ConditionTree equal(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, true);
	}

	public static ConditionTree notEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, false);
	}

	public static ConditionTree tryNull(ExpressionParser parser, InsnTree left, InsnTree right, boolean equal) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && leftConstant.asJavaObject() == null) {
			if (rightConstant.isConstant() && rightConstant.asJavaObject() == null) {
				return new ConstantConditionTree(equal);
			}
			else {
				return new NullCompareConditionTree(right.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW), equal ? IFNULL : IFNONNULL);
			}
		}
		else {
			if (rightConstant.isConstant() && rightConstant.asJavaObject() == null) {
				return new NullCompareConditionTree(left.cast(parser, TypeInfos.OBJECT, CastMode.IMPLICIT_THROW), equal ? IFNULL : IFNONNULL);
			}
			else {
				if (
					left.getTypeInfo().extendsOrImplements(right.getTypeInfo()) ||
					right.getTypeInfo().extendsOrImplements(left.getTypeInfo()) ||
					left.getTypeInfo().type.isInterface ||
					right.getTypeInfo().type.isInterface
				) {
					return new IdentityEqualityConditionTree(left, right, equal ? IF_ACMPEQ : IF_ACMPNE);
				}
				else {
					throw new IllegalArgumentException("Can't compare " + left.getTypeInfo() + " and " + right.getTypeInfo());
				}
			}
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
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