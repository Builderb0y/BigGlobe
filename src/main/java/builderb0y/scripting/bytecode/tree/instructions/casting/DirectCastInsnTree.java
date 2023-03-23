package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.UnaryInsnTree;

public class DirectCastInsnTree extends UnaryInsnTree {

	public TypeInfo type;

	public DirectCastInsnTree(InsnTree operand, TypeInfo type) {
		super(operand);
		this.type = type;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		method.node.visitTypeInsn(CHECKCAST, this.type.getInternalName());
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}
}