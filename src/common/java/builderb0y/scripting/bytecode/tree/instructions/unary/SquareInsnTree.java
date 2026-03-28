package builderb0y.scripting.bytecode.tree.instructions.unary;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class SquareInsnTree extends UnaryInsnTree {

	public SquareInsnTree(InsnTree value) {
		super(value);
	}

	public static InsnTree create(InsnTree value) {
		if (value.getTypeInfo().isNumber()) {
			return new SquareInsnTree(value);
		}
		else {
			throw new IllegalArgumentException(value.getTypeInfo() + " ^ 2");
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		method.node.visitInsn(this.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
		method.node.visitInsn(this.getTypeInfo().getOpcode(IMUL));
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.operand.getTypeInfo();
	}
}