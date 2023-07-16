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
		ConstantValue leftConstant  = left .getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (type.getSort()) {
				case INT    -> ldc(divideExact(parser, leftConstant.asInt (), rightConstant.asInt ()));
				case LONG   -> ldc(divideExact(parser, leftConstant.asLong(), rightConstant.asLong()));
				case FLOAT  -> ldc(leftConstant.asFloat () / rightConstant.asFloat ());
				case DOUBLE -> ldc(leftConstant.asDouble() / rightConstant.asDouble());
				default -> throw new AssertionError(type);
			};
		}
		left  = left .cast(parser, type, CastMode.EXPLICIT_THROW);
		right = right.cast(parser, type, CastMode.EXPLICIT_THROW);
		return new DivideInsnTree(left, right, type.getOpcode(IDIV));
	}

	public static int divideExact(ExpressionParser parser, int a, int b) throws ScriptParsingException {
		int div = a / b;
		if (div * b == a) return div;
		else throw new ScriptParsingException(a + " / " + b + " cannot be represented exactly as an int. Try doing " + a + ".0 / " + b + ".0 instead", parser.input);
	}

	public static long divideExact(ExpressionParser parser, long a, long b) throws ScriptParsingException {
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