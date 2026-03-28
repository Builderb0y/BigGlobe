package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class BlockInsnTree implements InsnTree {

	public LoopName loopName;
	public InsnTree body;

	public BlockInsnTree(LoopName loopName, InsnTree body) {
		this.loopName = loopName;
		this.body = body.asStatement();
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushLoop(this.loopName);
		this.body.emitBytecode(method);
		method.scopes.popLoop();
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