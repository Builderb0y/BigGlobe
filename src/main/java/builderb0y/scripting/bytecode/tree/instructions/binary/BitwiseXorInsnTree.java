package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BitwiseXorInsnTree extends BinaryInsnTree {

	public BitwiseXorInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.getSort() == Sort.BOOLEAN && right.getSort() == Sort.BOOLEAN) {
			return TypeInfos.BOOLEAN;
		}
		if (left.isInteger() && right.isInteger()) {
			return TypeInfos.widenUntilSame(left, right);
		}
		throw new InvalidOperandException("Cannot bitwise xor " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant  = left .getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case BOOLEAN -> ldc(        leftConstant.asBoolean() ^ rightConstant.asBoolean());
				case BYTE    -> ldc((byte )(leftConstant.asByte   () ^ rightConstant.asByte   ()));
				case SHORT   -> ldc((short)(leftConstant.asShort  () ^ rightConstant.asShort  ()));
				case INT     -> ldc(        leftConstant.asInt    () ^ rightConstant.asInt    ());
				case LONG    -> ldc(        leftConstant.asLong   () ^ rightConstant.asLong   ());
				default      -> throw new AssertionError(type);
			};
		}
		left  = left .cast(parser, type, CastMode.EXPLICIT_THROW);
		right = right.cast(parser, type, CastMode.EXPLICIT_THROW);
		return new BitwiseXorInsnTree(left, right, type.getOpcode(IXOR));
	}
}