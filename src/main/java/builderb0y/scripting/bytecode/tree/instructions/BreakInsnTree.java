package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class BreakInsnTree implements InsnTree {

	public static final BreakInsnTree INSTANCE = new BreakInsnTree();

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.node.visitJumpInsn(GOTO, method.scopes.findLoop().end.getLabel());
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return true;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}