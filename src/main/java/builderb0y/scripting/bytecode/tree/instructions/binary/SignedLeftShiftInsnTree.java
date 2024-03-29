package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SignedLeftShiftInsnTree extends BinaryInsnTree {

	public static final MethodInfo
		INT_SHIFT = MethodInfo.findMethod(SignedLeftShiftInsnTree.class, "shift", int.class, int.class, int.class).pure(),
		LONG_SHIFT = MethodInfo.findMethod(SignedLeftShiftInsnTree.class, "shift", long.class, long.class, int.class).pure(),
		FLOAT_SHIFT = MethodInfo.findMethod(Math.class, "scalb", float.class, float.class, int.class).pure(),
		DOUBLE_SHIFT = MethodInfo.findMethod(Math.class, "scalb", double.class, double.class, int.class).pure();

	public SignedLeftShiftInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isSingleWidthInt()) {
			return TypeInfos.widenToInt(left);
		}
		throw new InvalidOperandException("Can't signed left shift " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case INT    -> ldc(     shift(leftConstant.   asInt(), rightConstant.asInt()));
				case LONG   -> ldc(     shift(leftConstant.  asLong(), rightConstant.asInt()));
				case FLOAT  -> ldc(Math.scalb(leftConstant. asFloat(), rightConstant.asInt()));
				case DOUBLE -> ldc(Math.scalb(leftConstant.asDouble(), rightConstant.asInt()));
				default     -> throw new AssertionError(type);
			};
		}
		left = left.cast(parser, type, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
		return new SignedLeftShiftInsnTree(left, right, switch (type.getSort()) {
			case INT -> ISHL;
			case LONG -> LSHL;
			case FLOAT -> FSHL;
			case DOUBLE -> DSHL;
			default -> throw new AssertionError(type);
		});
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		TypeInfo leftType = this.left.getTypeInfo();
		ConstantValue rightConstant = this.right.getConstantValue();
		if (leftType.isInteger()) {
			if (rightConstant.isConstant()) {
				int maxShift = leftType.isDoubleWidth() ? 64 : 32;
				long shift = rightConstant.asLong();
				if (shift > 0) {
					if (shift >= maxShift) {
						this.left.emitBytecode(method);
						method.node.visitInsn(leftType.isDoubleWidth() ? POP2 : POP);
						constant(0, leftType).emitBytecode(method);
					}
					else {
						this.left.emitBytecode(method);
						this.right.emitBytecode(method);
						method.node.visitInsn(leftType.getOpcode(ISHL));
					}
				}
				else if (shift < 0) {
					if (shift <= -maxShift) {
						shift = 1 - maxShift;
					}
					this.left.emitBytecode(method);
					ldc(-shift, TypeInfos.INT).emitBytecode(method);
					method.node.visitInsn(leftType.getOpcode(ISHR));
				}
				else {
					this.left.emitBytecode(method);
				}
			}
			else {
				invokeStatic(
					leftType.getSort() == Sort.LONG ? LONG_SHIFT : INT_SHIFT,
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
					float multiplier = Math.scalb(1.0F, rightConstant.asInt());
					if (multiplier != 0.0F && Float.isFinite(multiplier)) {
						new MultiplyInsnTree(this.left, ldc(multiplier), FMUL).emitBytecode(method);
						return;
					}
				}
				else {
					double multiplier = Math.scalb(1.0D, rightConstant.asInt());
					if (multiplier != 0.0D && Double.isFinite(multiplier)) {
						new MultiplyInsnTree(this.left, ldc(multiplier), DMUL).emitBytecode(method);
						return;
					}
				}
			}
			invokeStatic(
				leftType.isDoubleWidth() ? DOUBLE_SHIFT : FLOAT_SHIFT,
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
			return b <= -31 ? a >> 31 : a >> -b;
		}
	}

	public static long shift(long a, int b) {
		if (b >= 0) {
			return b >= 64 ? 0 : a << b;
		}
		else {
			return b <= -63 ? a >> 63 : a >> -b;
		}
	}

	//this code was fun to write, but the float version is
	//multiple orders of magnitude slower than Math.scalb().
	//additionally, it has different semantics for
	//rounding subnormal numbers than scalb() does.
	//in order to ensure consistency,
	//I will use scalb() for both floats and doubles.
	//the below two methods are not used by generated bytecode,
	//but I'm keeping them here anyway because I put a lot of work into them.

	public static float shift(float a, int b) {
		if (a == 0.0F || !Float.isFinite(a) || b == 0) return a;
		int bits = Float.floatToRawIntBits(a);
		int exponent = bits << 1 >>> 24;
		int mantissa = bits & 0x007FFFFF;
		if (exponent == 0) { //subnormal input.
			exponent = 8 - Integer.numberOfLeadingZeros(mantissa);
			mantissa = (mantissa << -exponent) & 0x007FFFFF;
			exponent++;
		}
		//overflow-conscious code.
		exponent += Math.min(Math.max(b, -22 - exponent), 255 - exponent);
		if (exponent <= 0) { //subnormal output.
			exponent--;
			mantissa = (mantissa | 0x00800000) >>> -exponent;
			exponent = 0;
		}
		else if (exponent >= 255) { //infinite output.
			return Float.intBitsToFloat((bits & 0x80000000) | 0x7F800000);
		}
		return Float.intBitsToFloat((bits & 0x80000000) | (exponent << 23) | mantissa);
	}

	public static double shift(double a, int b) {
		if (a == 0.0D || !Double.isFinite(a) || b == 0) return a;
		long bits = Double.doubleToRawLongBits(a);
		int exponent = (int)(bits << 1 >>> 53);
		long mantissa = bits & 0x000F_FFFF_FFFF_FFFFL;
		if (exponent == 0) { //subnormal input.
			exponent = 11 - Long.numberOfLeadingZeros(mantissa);
			mantissa = (mantissa << -exponent) & 0x000F_FFFF_FFFF_FFFFL;
			exponent++;
		}
		exponent += Math.min(Math.max(b, -51 - exponent), 2047 - exponent);
		if (exponent <= 0) { //subnormal output.
			exponent--;
			mantissa = (mantissa | 0x0010_0000_0000_0000L) >>> -exponent;
			exponent = 0;
		}
		else if (exponent >= 2047) { //infinite output.
			return Double.longBitsToDouble((bits & 0x8000_0000_0000_0000L) | 0x7FF0_0000_0000_0000L);
		}
		return Double.longBitsToDouble((bits & 0x8000_0000_0000_0000L) | (((long)(exponent)) << 52) | mantissa);
	}
}