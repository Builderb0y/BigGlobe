package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class MultiplyInsnTree extends BinaryInsnTree {

	public MultiplyInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isNumber()) {
			return TypeInfos.widenUntilSameInt(left, right);
		}
		throw new InvalidOperandException("Can't multiply " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		ConstantValue scalar;
		InsnTree variable;
		if (leftConstant.isConstant()) {
			if (rightConstant.isConstant()) {
				return switch (type.getSort()) {
					case INT -> ldc(Math.multiplyExact(leftConstant.asInt(), rightConstant.asInt()));
					case LONG -> ldc(Math.multiplyExact(leftConstant.asLong(), rightConstant.asLong()));
					case FLOAT -> ldc(leftConstant.asFloat() * rightConstant.asFloat());
					case DOUBLE -> ldc(leftConstant.asDouble() * rightConstant.asDouble());
					default -> throw new AssertionError(type);
				};
			}
			else {
				scalar = leftConstant;
				variable = right;
			}
		}
		else {
			if (rightConstant.isConstant()) {
				scalar = rightConstant;
				variable = left;
			}
			else {
				scalar = null;
				variable = null;
			}
		}
		if (scalar != null) {
			switch (type.getSort()) {
				case INT -> {
					int intScalar = scalar.asInt();
					if (intScalar != 0 && (intScalar & (intScalar - 1)) == 0) {
						int shift = Integer.numberOfTrailingZeros(intScalar);
						return new SignedLeftShiftInsnTree(variable, ldc(shift), ISHL);
					}
				}
				case LONG -> {
					long longScalar = scalar.asLong();
					if (longScalar != 0L && (longScalar & (longScalar - 1L)) == 0) {
						int shift = Long.numberOfTrailingZeros(longScalar);
						return new SignedLeftShiftInsnTree(variable, ldc(shift), LSHL);
					}
				}
				default -> {}
			}
		}
		left = left.cast(parser, type, CastMode.EXPLICIT_THROW, false);
		right = right.cast(parser, type, CastMode.EXPLICIT_THROW, false);
		return new MultiplyInsnTree(left, right, type.getOpcode(IMUL));
	}
}