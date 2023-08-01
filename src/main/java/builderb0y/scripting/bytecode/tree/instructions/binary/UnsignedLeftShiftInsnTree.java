package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class UnsignedLeftShiftInsnTree extends BinaryInsnTree {

	public static final MethodInfo
		INT_SHIFT = MethodInfo.findMethod(UnsignedLeftShiftInsnTree.class, "shift", int.class, int.class, int.class).pure(),
		LONG_SHIFT = MethodInfo.findMethod(UnsignedLeftShiftInsnTree.class, "shift", long.class, long.class, int.class).pure();

	public UnsignedLeftShiftInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isInteger() && right.isSingleWidthInt()) {
			return TypeInfos.widenToInt(left);
		}
		throw new InvalidOperandException("Can't unsigned left shift " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case INT  -> ldc(shift(leftConstant. asInt(), rightConstant.asInt()));
				case LONG -> ldc(shift(leftConstant.asLong(), rightConstant.asInt()));
				default   -> throw new AssertionError(type);
			};
		}
		left = left.cast(parser, type, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
		return new UnsignedLeftShiftInsnTree(left, right, switch (type.getSort()) {
			case INT -> IUSHL;
			case LONG -> LUSHL;
			default -> throw new AssertionError(type);
		});
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		TypeInfo leftType = this.left.getTypeInfo();
		int maxShift = leftType.isDoubleWidth() ? 64 : 32;
		ConstantValue rightConstant = this.right.getConstantValue();
		if (rightConstant.isConstant()) {
			this.left.emitBytecode(method);
			long shift = rightConstant.asLong();
			if (shift > 0) {
				if (shift >= maxShift) {
					method.node.visitInsn(leftType.isDoubleWidth() ? POP2 : POP);
					constant(0, leftType).emitBytecode(method);
				}
				else {
					this.right.emitBytecode(method);
					method.node.visitInsn(leftType.getOpcode(ISHL));
				}
			}
			else if (shift < 0) {
				if (shift <= -maxShift) {
					method.node.visitInsn(leftType.isDoubleWidth() ? POP2 : POP);
					constant(0, leftType).emitBytecode(method);
				}
				else {
					ldc(-shift, TypeInfos.INT).emitBytecode(method);
					method.node.visitInsn(leftType.getOpcode(IUSHR));
				}
			}
			//else leave left unchanged.
		}
		else {
			invokeStatic(
				leftType.isDoubleWidth() ? LONG_SHIFT : INT_SHIFT,
				this.left,
				this.right
			)
			.emitBytecode(method);
		}
	}

	public static int shift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? 0 : a << b;
		}
		else {
			return b <= -31 ? 0 : a >>> -b;
		}
	}

	public static long shift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? 0 : a << b;
		}
		else {
			return b <= -63 ? 0 : a >>> -b;
		}
	}
}