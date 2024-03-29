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
		ConstantValue leftConstant  = left .getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case INT    -> ldc(Math.addExact(leftConstant. asInt(), rightConstant. asInt()));
				case LONG   -> ldc(Math.addExact(leftConstant.asLong(), rightConstant.asLong()));
				case FLOAT  -> ldc(leftConstant. asFloat() + rightConstant. asFloat());
				case DOUBLE -> ldc(leftConstant.asDouble() + rightConstant.asDouble());
				default     -> throw new AssertionError(type);
			};
		}
		left  = left .cast(parser, type, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, type, CastMode.IMPLICIT_THROW);
		return new AddInsnTree(left, right, type.getOpcode(IADD));
	}
}