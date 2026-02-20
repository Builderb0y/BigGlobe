package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DivideInsnTree extends BinaryInsnTree {

	public DivideInsnTree(InsnTree left, InsnTree right, int opcode) {
		super(left, right, opcode);
	}

	public static TypeInfo validate(TypeInfo left, TypeInfo right) {
		if (left.isNumber() && right.isNumber()) {
			return TypeInfos.widenUntilSameInt(left, right);
		}
		throw new InvalidOperandException("Can't divide " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) throws ScriptParsingException {
		TypeInfo type = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue rightConstant = right.getConstantValue();
		if (rightConstant.isConstant()) {
			ConstantValue leftConstant  = left.getConstantValue();
			if (leftConstant.isConstant()) {
				return switch (type.getSort()) {
					case INT -> ldc(divideExact(parser, leftConstant.asInt(), rightConstant.asInt()));
					case LONG -> ldc(divideExact(parser, leftConstant.asLong(), rightConstant.asLong()));
					case FLOAT -> ldc(leftConstant.asFloat() / rightConstant.asFloat());
					case DOUBLE -> ldc(leftConstant.asDouble() / rightConstant.asDouble());
					default -> throw new AssertionError(type);
				};
			}
			else {
				switch (type.getSort()) {
					case INT -> {
						int intScalar = rightConstant.asInt();
						if (intScalar == 0) {
							throw new ArithmeticException("Division by literal zero");
						}
						else if (intScalar > 0 && (intScalar & (intScalar - 1)) == 0) {
							int shift = Integer.numberOfTrailingZeros(intScalar);
							return new SignedRightShiftInsnTree(left, ldc(shift), ISHR);
						}
					}
					case LONG -> {
						long longScalar = rightConstant.asLong();
						if (longScalar == 0L) {
							throw new ArithmeticException("Division by literal zero");
						}
						else if (longScalar > 0L && (longScalar & (longScalar - 1L)) == 0L) {
							int shift = Long.numberOfTrailingZeros(longScalar);
							return new SignedRightShiftInsnTree(left, ldc(shift), LSHR);
						}
					}
					default -> {}
				}
			}
		}
		left  = left .cast(parser, type, CastMode.EXPLICIT_THROW, false);
		right = right.cast(parser, type, CastMode.EXPLICIT_THROW, false);
		return new DivideInsnTree(left, right, type.getOpcode(IDIV));
	}

	public static int divideExact(ExpressionParser parser, int a, int b) throws ScriptParsingException {
		if (b == 0) throw new ScriptParsingException("Division by literal zero", parser.input);
		int div = a / b;
		if (div * b == a) return div;
		else throw new ScriptParsingException(a + " / " + b + " cannot be represented exactly as an int. Try doing " + a + ".0 / " + b + ".0 instead", parser.input);
	}

	public static long divideExact(ExpressionParser parser, long a, long b) throws ScriptParsingException {
		if (b == 0L) throw new ScriptParsingException("Division by literal zero", parser.input);
		long div = a / b;
		if (div * b == a) return div;
		else throw new ScriptParsingException(a + " / " + b + " cannot be represented exactly as a long. Try doing " + a + ".0 / " + b + ".0 instead", parser.input);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		switch (this.opcode) {
			case IDIV -> method.node.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(II)I", false);
			case LDIV -> method.node.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(JJ)J", false);
			case FDIV -> method.node.visitInsn(FDIV);
			case DDIV -> method.node.visitInsn(DDIV);
			default   -> throw new AssertionError(this.opcode);
		}
	}
}