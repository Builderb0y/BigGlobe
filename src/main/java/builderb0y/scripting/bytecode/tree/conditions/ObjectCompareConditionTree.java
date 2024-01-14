package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.ArrayExtensions;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ObjectCompareConditionTree implements ConditionTree {

	public static final MethodInfo
		COMPARE_G              = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "compareG", TypeInfos.INT, TypeInfos.COMPARABLE, TypeInfos.COMPARABLE),
		COMPARE_L              = new MethodInfo(ACC_PUBLIC | ACC_STATIC | MethodInfo.PURE, type(ObjectCompareConditionTree.class), "compareL", TypeInfos.INT, TypeInfos.COMPARABLE, TypeInfos.COMPARABLE);

	public final InsnTree loader;
	public final int opcode;

	public ObjectCompareConditionTree(InsnTree loader, int opcode) {
		this.loader = loader;
		this.opcode = opcode;
	}

	public static ConditionTree equal(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, true);
	}

	public static ConditionTree notEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return tryNull(parser, left, right, false);
	}

	public static ConditionTree lessThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_G, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFLT);
	}

	public static ConditionTree greaterThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_L, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFGT);
	}

	public static ConditionTree lessThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_G, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFLE);
	}

	public static ConditionTree greaterThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		return new ObjectCompareConditionTree(invokeStatic(COMPARE_L, left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW), right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_THROW)), IFGE);
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.loader.emitBytecode(method);
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

	public static ConditionTree tryNull(ExpressionParser parser, InsnTree left, InsnTree right, boolean equal) {
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && leftConstant.asJavaObject() == null) {
			if (rightConstant.isConstant() && rightConstant.asJavaObject() == null) {
				return ConstantConditionTree.of(equal);
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
					return new ObjectCompareConditionTree(equalLoader(parser, left, right), equal ? IFNE : IFEQ);
				}
				else {
					throw new IllegalArgumentException("Can't compare " + left.getTypeInfo() + " and " + right.getTypeInfo());
				}
			}
		}
	}

	public static InsnTree equalLoader(ExpressionParser parser, InsnTree left, InsnTree right) {
		return ArrayExtensions.computeEquals(parser, left, right);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compareG(Comparable a, Comparable b) {
		return a == null || b == null ? 1 : a.compareTo(b);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static int compareL(Comparable a, Comparable b) {
		return a == null || b == null ? -1 : a.compareTo(b);
	}
}