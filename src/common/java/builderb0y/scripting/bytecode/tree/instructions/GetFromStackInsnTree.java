package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

/**
used to retrieve a value which is already on the stack.
this InsnTree is used internally by updater logic.
*/
public class GetFromStackInsnTree implements InsnTree {

	public TypeInfo type;

	public GetFromStackInsnTree(TypeInfo type) {
		this.type = type;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//nothing to do here.
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}
}