package builderb0y.scripting.bytecode.tree.instructions.binary;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.math.FastPow;
import builderb0y.scripting.bytecode.ExtendedOpcodes;
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

public abstract class PowerInsnTree extends BinaryInsnTree {

	public final PowMode mode;

	public PowerInsnTree(InsnTree left, InsnTree right, PowMode mode) {
		super(left, right, mode.opcode);
		this.mode = mode;
	}

	public static PowMode validate(TypeInfo left, TypeInfo right) {
		PowMode mode = switch (TypeInfos.widenToInt(right).getSort()) {
			case INT -> switch (TypeInfos.widenToInt(left).getSort()) {
				case INT -> PowMode.IIPOW;
				case LONG -> PowMode.LIPOW;
				case FLOAT -> PowMode.FIPOW;
				case DOUBLE -> PowMode.DIPOW;
				default -> null;
			};
			case FLOAT -> {
				if (left.isNumber()) {
					yield left.getSort() == Sort.DOUBLE ? PowMode.DDPOW : PowMode.FFPOW;
				}
				yield null;
			}
			case DOUBLE -> {
				if (left.isNumber()) {
					yield PowMode.DDPOW;
				}
				yield null;
			}
			default -> null;
		};
		if (mode != null) {
			return mode;
		}
		throw new InvalidOperandException("Can't pow " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		PowMode mode = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (mode) {
				case IIPOW -> ldc(FastPow.pow(leftConstant.   asInt(), rightConstant.   asInt()));
				case LIPOW -> ldc(FastPow.pow(leftConstant.  asLong(), rightConstant.   asInt()));
				case FIPOW -> ldc(FastPow.pow(leftConstant. asFloat(), rightConstant.   asInt()));
				case DIPOW -> ldc(FastPow.pow(leftConstant.asDouble(), rightConstant.   asInt()));
				case FFPOW -> ldc(FastPow.pow(leftConstant. asFloat(), rightConstant. asFloat()));
				case DDPOW -> ldc(   Math.pow(leftConstant.asDouble(), rightConstant.asDouble()));
			};
		}
		left  = left .cast(parser, mode.leftType,  CastMode.EXPLICIT_THROW);
		right = right.cast(parser, mode.rightType, CastMode.EXPLICIT_THROW);
		if (rightConstant.isConstant()) {
			return new VariableConstantPowerInsnTree(left, right, mode);
		}
		if (leftConstant.isConstant()) {
			return new ConstantVariablePowerInsnTree(left, right, mode);
		}
		return new VariableVariablePowerInsnTree(left, right, mode);
	}

	public void emitFallbackBytecode(MethodCompileContext method) {
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitMethodInsn(
			INVOKESTATIC,
			Type.getInternalName(this.mode == PowMode.DDPOW ? Math.class : FastPow.class),
			"pow",
			switch (this.mode) {
				case IIPOW -> "(II)I";
				case LIPOW -> "(JI)J";
				case FIPOW -> "(FI)F";
				case DIPOW -> "(DI)D";
				case FFPOW -> "(FF)F";
				case DDPOW -> "(DD)D";
				default    -> throw new AssertionError(this.mode);
			},
			false
		);
	}

	public static class VariableConstantPowerInsnTree extends PowerInsnTree {

		public VariableConstantPowerInsnTree(InsnTree left, InsnTree right, PowMode mode) {
			super(left, right, mode);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			double power = this.right.getConstantValue().asDouble();
			int intPower = (int)(power);
			if (intPower == power) {
				if (intPower == 2) { //special handle x^2
					this.left.emitBytecode(method);
					method.node.visitInsn(this.left.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
					method.node.visitInsn(this.left.getTypeInfo().getOpcode(IMUL));
				}
				else { //use invokedynamic for x ^ (constant int)
					this.left.emitBytecode(method);
					method.node.visitInvokeDynamicInsn(
						"pow",
						switch (this.mode) {
							case IIPOW -> "(I)I";
							case LIPOW -> "(J)J";
							case FIPOW, FFPOW -> "(F)F";
							case DIPOW, DDPOW -> "(D)D";
						},
						new Handle(
							H_INVOKESTATIC,
							Type.getInternalName(FastPow.class),
							"getCallSite",
							"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;",
							false
						),
						intPower
					);
				}
			}
			else { //x ^ (non-int)
				this.emitFallbackBytecode(method);
			}
		}
	}

	public static class ConstantVariablePowerInsnTree extends PowerInsnTree {

		public static final MethodInfo
			EXPD = MethodInfo.getMethod(Math.class, "exp"),
			EXPF = MethodInfo.getMethod(FastPow.class, "exp");

		public ConstantVariablePowerInsnTree(InsnTree left, InsnTree right, PowMode mode) {
			super(left, right, mode);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			double base = this.left.getConstantValue().asDouble();
			if (base > 0.0D && base < Double.POSITIVE_INFINITY) {
				double logBase = Math.log(base);
				switch (this.mode) {
					case FFPOW -> {
						this.right.emitBytecode(method);
						constant((float)(logBase)).emitBytecode(method);
						method.node.visitInsn(FMUL);
						EXPF.emitBytecode(method);
					}
					case DDPOW -> {
						this.right.emitBytecode(method);
						constant(logBase).emitBytecode(method);
						method.node.visitInsn(DMUL);
						EXPD.emitBytecode(method);
					}
					default -> {
						this.emitFallbackBytecode(method);
					}
				}
			}
			else {
				this.emitFallbackBytecode(method);
			}
		}
	}

	public static class VariableVariablePowerInsnTree extends PowerInsnTree {

		public VariableVariablePowerInsnTree(InsnTree left, InsnTree right, PowMode mode) {
			super(left, right, mode);
		}

		@Override
		public void emitBytecode(MethodCompileContext method) {
			this.emitFallbackBytecode(method);
		}
	}

	public static enum PowMode {
		IIPOW(ExtendedOpcodes.IIPOW, TypeInfos.INT, TypeInfos.INT),
		LIPOW(ExtendedOpcodes.LIPOW, TypeInfos.LONG, TypeInfos.INT),
		FIPOW(ExtendedOpcodes.FIPOW, TypeInfos.FLOAT, TypeInfos.INT),
		DIPOW(ExtendedOpcodes.DIPOW, TypeInfos.DOUBLE, TypeInfos.INT),
		FFPOW(ExtendedOpcodes.FFPOW, TypeInfos.FLOAT, TypeInfos.FLOAT),
		DDPOW(ExtendedOpcodes.DDPOW, TypeInfos.DOUBLE, TypeInfos.DOUBLE);

		public final int opcode;
		public final TypeInfo leftType, rightType;

		PowMode(int opcode, TypeInfo leftType, TypeInfo rightType) {
			this.opcode = opcode;
			this.leftType = leftType;
			this.rightType = rightType;
		}
	}
}