package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.UnaryInsnTree;

public class OpcodeCastInsnTree extends UnaryInsnTree {

	public int opcode;
	public TypeInfo type;

	public OpcodeCastInsnTree(InsnTree operand, int opcode, TypeInfo type) {
		super(operand);
		this.opcode = opcode;
		this.type = type;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		method.node.visitInsn(this.opcode);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public boolean canBeStatement() {
		return this.operand.canBeStatement() && this.type.isVoid();
	}
}