package builderb0y.scripting.bytecode.tree.instructions.binary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

public abstract class BinaryInsnTree implements InsnTree {

	public InsnTree left, right;
	public int opcode;

	public BinaryInsnTree(InsnTree left, InsnTree right, int opcode) {
		this.left = left;
		this.right = right;
		this.opcode = opcode;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitInsn(this.opcode);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.left.getTypeInfo();
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return this.left.then(parser, this.right).then(parser, nextStatement);
	}

	public static boolean isPowerOf2(long value) {
		return value > 0L && (value & (value - 1L)) == 0L;
	}
}