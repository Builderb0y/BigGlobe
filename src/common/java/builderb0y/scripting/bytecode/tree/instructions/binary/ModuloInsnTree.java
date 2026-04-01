package builderb0y.scripting.bytecode.tree.instructions.binary;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ModuloInsnTree extends BinaryInsnTree {

	public ModuloInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isNumber()) {
			return TypeInfos.widenUntilSameInt(left, right);
		}
		throw new InvalidOperandException("Can't modulo " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue rightConstant = right.getConstantValue();
		if (rightConstant.isConstant()) {
			ConstantValue leftConstant = left.getConstantValue();
			if (leftConstant.isConstant()) {
				return switch (type.getSort()) {
					case INT -> ldc(BigGlobeMath.modulus(leftConstant.asInt(), rightConstant.asInt()));
					case LONG -> ldc(BigGlobeMath.modulus(leftConstant.asLong(), rightConstant.asLong()));
					case FLOAT -> ldc(BigGlobeMath.modulus(leftConstant.asFloat(), rightConstant.asFloat()));
					case DOUBLE -> ldc(BigGlobeMath.modulus(leftConstant.asDouble(), rightConstant.asDouble()));
					default -> throw new AssertionError(type);
				};
			}
			else {
				switch (type.getSort()) {
					case INT -> {
						int intMod = rightConstant.asInt();
						if (intMod > 0 && (intMod & (intMod - 1)) == 0) {
							int mask = intMod - 1;
							return new BitwiseAndInsnTree(left, ldc(mask), IAND);
						}
					}
					case LONG -> {
						long longMod = rightConstant.asLong();
						if (longMod > 0L && (longMod & (longMod - 1L)) == 0) {
							long mask = longMod - 1L;
							return new BitwiseAndInsnTree(left, ldc(mask), LAND);
						}
					}
					default -> {}
				}
			}
		}
		left = left.cast(parser, type, CastMode.EXPLICIT_THROW, false);
		right = right.cast(parser, type, CastMode.EXPLICIT_THROW, false);
		return new ModuloInsnTree(left, right, type.getOpcode(IREM));
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		String methodName = "modulus";
		ConstantValue rightConstant = this.right.getConstantValue();
		if (rightConstant.isConstant()) {
			double value = rightConstant.asDouble();
			if (value > 0.0D) {
				methodName = "modulus_BP";
			}
			else if (value < 0.0D) {
				methodName = "modulus_BN";
			}
			else { //right value is 0 or NaN.
				this.left.emitBytecode(method);
				method.node.visitInsn(switch (this.opcode) {
					case IREM, FREM -> POP;
					case LREM, DREM -> POP2;
					default -> throw new AssertionError(this.opcode);
				});
				this.right.emitBytecode(method);
				return;
			}
		}
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitMethodInsn(
			INVOKESTATIC,
			Type.getInternalName(BigGlobeMath.class),
			methodName,
			switch (this.opcode) {
				case IREM -> "(II)I";
				case LREM -> "(JJ)J";
				case FREM -> "(FF)F";
				case DREM -> "(DD)D";
				default -> throw new AssertionError(this.opcode);
			},
			false
		);
	}
}