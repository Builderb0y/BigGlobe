package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class ContinueInsnTree implements InsnTree {

	public String loopName;

	public ContinueInsnTree(String loopName) {
		this.loopName = loopName;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.node.visitJumpInsn(GOTO, method.scopes.findLoopForContinue(this.loopName).getContinuePoint().getLabel());
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