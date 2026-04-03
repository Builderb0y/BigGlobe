package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class ForcedStatementInsnTree implements InsnTree {

	public final InsnTree operand;

	public ForcedStatementInsnTree(InsnTree operand) {
		this.operand = operand;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
		switch (this.operand.getTypeInfo().getSize()) {
			case 0 -> {}
			case 1 -> method.node.visitInsn(POP);
			case 2 -> method.node.visitInsn(POP2);
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}