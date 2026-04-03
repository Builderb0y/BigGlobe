package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AddInsnTree extends BinaryInsnTree {

	public AddInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isNumber()) {
			return TypeInfos.widenUntilSameInt(left, right);
		}
		throw new InvalidOperandException("Can't add " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant()) {
			if (rightConstant.isConstant()) {
				return switch (type.getSort()) {
					case INT -> ldc(Math.addExact(leftConstant.asInt(), rightConstant.asInt()));
					case LONG -> ldc(Math.addExact(leftConstant.asLong(), rightConstant.asLong()));
					case FLOAT -> ldc(leftConstant.asFloat() + rightConstant.asFloat());
					case DOUBLE -> ldc(leftConstant.asDouble() + rightConstant.asDouble());
					default -> throw new AssertionError(type);
				};
			}
			else {
				//can't apply this optimization to floats or doubles,
				//because 0.0 + -0.0 = 0.0.
				if (type.isInteger() && leftConstant.asLong() == 0L) {
					return right;
				}
			}
		}
		else {
			if (rightConstant.isConstant()) {
				//can't apply this optimization to floats or doubles,
				//because -0.0 + 0.0 = 0.0.
				if (type.isInteger() && rightConstant.asLong() == 0L) {
					return left;
				}
			}
			else {
				//no constants. fallthrough.
			}
		}
		left = left.cast(parser, type, CastMode.IMPLICIT_THROW, false);
		right = right.cast(parser, type, CastMode.IMPLICIT_THROW, false);
		return new AddInsnTree(left, right, type.getOpcode(IADD));
	}
}