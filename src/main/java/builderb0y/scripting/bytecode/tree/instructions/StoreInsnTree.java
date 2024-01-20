package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class StoreInsnTree implements InsnTree {

	public LazyVarInfo variable;
	public InsnTree value;

	public StoreInsnTree(LazyVarInfo variable, InsnTree value) {
		this.variable = variable;
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
		this.variable.emitStore(method);
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