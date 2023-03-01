package builderb0y.scripting.bytecode.tree.conditions;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class CompareConditionTree {

	public static record Operands(InsnTree left, InsnTree right, TypeInfo type) {

		public static Operands of(ExpressionParser parser, InsnTree left, InsnTree right, TypeInfo type) {
			return new Operands(left.cast(parser, type, CastMode.IMPLICIT_THROW), right.cast(parser, type, CastMode.IMPLICIT_THROW), type);
		}
	}

	public static Operands operands(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo leftType = left.getTypeInfo();
		TypeInfo rightType = right.getTypeInfo();
		if (leftType.isNumber() && rightType.isNumber()) {
			return Operands.of(parser, left, right, TypeInfos.widenUntilSameInt(leftType, rightType));
		}
		InsnTree leftComparable = left.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_NULL);
		if (leftComparable != null) {
			InsnTree rightComparable = right.cast(parser, TypeInfos.COMPARABLE, CastMode.IMPLICIT_NULL);
			if (rightComparable != null) {
				return new Operands(leftComparable, rightComparable, TypeInfos.COMPARABLE);
			}
		}
		return Operands.of(parser, left, right, TypeInfos.OBJECT);
	}

	public static ConditionTree equal(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.equal(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.equal(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.equal(operands.left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.equal(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.equal(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}

	public static ConditionTree notEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.notEqual(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.notEqual(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.notEqual(operands.left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.notEqual(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.notEqual(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}

	public static ConditionTree lessThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.lessThan(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.lessThan(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.lessThan(operands.left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.lessThan(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.lessThan(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}

	public static ConditionTree greaterThan(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.greaterThan(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.greaterThan(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.greaterThan(operands.left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.greaterThan(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.greaterThan(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}

	public static ConditionTree lessThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.lessThanOrEqual(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.lessThanOrEqual(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.lessThanOrEqual(left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.lessThanOrEqual(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.lessThanOrEqual(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}

	public static ConditionTree greaterThanOrEqual(ExpressionParser parser, InsnTree left, InsnTree right) {
		Operands operands = operands(parser, left, right);
		return switch (operands.type.getSort()) {
			case INT    ->    IntCompareConditionTree.greaterThanOrEqual(operands.left, operands.right);
			case LONG   ->   LongCompareConditionTree.greaterThanOrEqual(operands.left, operands.right);
			case FLOAT  ->  FloatCompareConditionTree.greaterThanOrEqual(operands.left, operands.right);
			case DOUBLE -> DoubleCompareConditionTree.greaterThanOrEqual(operands.left, operands.right);
			case OBJECT -> ObjectCompareConditionTree.greaterThanOrEqual(parser, operands.left, operands.right);
			default -> throw new InvalidOperandException("Can't compare " + operands.type);
		};
	}
}