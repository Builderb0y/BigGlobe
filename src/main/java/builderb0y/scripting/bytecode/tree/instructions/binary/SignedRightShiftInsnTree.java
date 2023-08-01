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

public class SignedRightShiftInsnTree extends BinaryInsnTree {

	public static final MethodInfo
		INT_SHIFT = MethodInfo.findMethod(SignedRightShiftInsnTree.class, "shift", int.class, int.class, int.class).pure(),
		LONG_SHIFT = MethodInfo.findMethod(SignedRightShiftInsnTree.class, "shift", long.class, long.class, int.class).pure(),
		FLOAT_SHIFT = MethodInfo.findMethod(Math.class, "scalb", float.class, float.class, int.class).pure(),
		DOUBLE_SHIFT = MethodInfo.findMethod(Math.class, "scalb", double.class, double.class, int.class).pure(),
		NEGATE = MethodInfo.getMethod(SignedRightShiftInsnTree.class, "negate");

	public SignedRightShiftInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isSingleWidthInt()) {
			return TypeInfos.widenToInt(left);
		}
		throw new InvalidOperandException("Can't signed right shift " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case INT    -> ldc(     shift(leftConstant.   asInt(),        rightConstant.asInt() ));
				case LONG   -> ldc(     shift(leftConstant.  asLong(),        rightConstant.asInt() ));
				case FLOAT  -> ldc(Math.scalb(leftConstant. asFloat(), negate(rightConstant.asInt())));
				case DOUBLE -> ldc(Math.scalb(leftConstant.asDouble(), negate(rightConstant.asInt())));
				default     -> throw new AssertionError(type);
			};
		}
		left = left.cast(parser, type, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
		return new SignedRightShiftInsnTree(left, right, switch (type.getSort()) {
			case INT -> ISHR;
			case LONG -> LSHR;
			case FLOAT -> FSHR;
			case DOUBLE -> DSHR;
			default -> throw new AssertionError(type);
		});
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		TypeInfo leftType = this.left.getTypeInfo();
		ConstantValue rightConstant = this.right.getConstantValue();
		if (leftType.isInteger()) {
			int maxShift = leftType.isDoubleWidth() ? 64 : 32;
			if (rightConstant.isConstant()) {
				long shift = rightConstant.asLong();
				if (shift > 0) {
					if (shift >= maxShift) {
						shift = maxShift - 1;
					}
					this.left.emitBytecode(method);
					ldc(shift, TypeInfos.INT).emitBytecode(method);
					method.node.visitInsn(leftType.getOpcode(ISHR));
				}
				else if (shift < 0) {
					if (shift <= -maxShift) {
						this.left.emitBytecode(method);
						method.node.visitInsn(leftType.isDoubleWidth() ? POP2 : POP);
						constant(0, leftType).emitBytecode(method);
					}
					else {
						this.left.emitBytecode(method);
						ldc(-shift, TypeInfos.INT).emitBytecode(method);
						method.node.visitInsn(leftType.getOpcode(ISHL));
					}
				}
				else {
					this.left.emitBytecode(method);
				}
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
		else {
			if (rightConstant.isConstant()) {
				if (rightConstant.asInt() == 0) {
					this.left.emitBytecode(method);
					return;
				}
				if (leftType.isSingleWidth()) {
					float multiplier = Math.scalb(1.0F, negate(rightConstant.asInt()));
					if (multiplier != 0.0F && Float.isFinite(multiplier)) {
						new MultiplyInsnTree(this.left, ldc(multiplier), FMUL).emitBytecode(method);
						return;
					}
				}
				else {
					double multiplier = Math.scalb(1.0D, negate(rightConstant.asInt()));
					if (multiplier != 0.0D && Double.isFinite(multiplier)) {
						new MultiplyInsnTree(this.left, ldc(multiplier), DMUL).emitBytecode(method);
						return;
					}
				}
			}
			invokeStatic(
				leftType.isDoubleWidth() ? DOUBLE_SHIFT : FLOAT_SHIFT,
				this.left,
				invokeStatic(NEGATE, this.right)
			)
			.emitBytecode(method);
		}
	}

	public static int shift(int a, int b) {
		if (b >= 0) {
			return b >= 32 ? a >> 31 : a >> b;
		}
		else {
			return b <= -31 ? 0 : a << -b;
		}
	}

	public static long shift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? a >> 63 : a >> b;
		}
		else {
			return b <= -63 ? 0 : a << -b;
		}
	}

	public static int negate(int value) {
		if (value == Integer.MIN_VALUE) return Integer.MAX_VALUE;
		return -value;
	}
}