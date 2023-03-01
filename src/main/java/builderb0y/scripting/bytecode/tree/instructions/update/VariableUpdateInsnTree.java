package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class VariableUpdateInsnTree extends UpdateInsnTree {

	public VarInfo variable;

	public VariableUpdateInsnTree(VarInfo variable, InsnTree updater) {
		super(updater);
		this.variable = variable;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.variable.emitLoad(method.node);
		this.updater.emitBytecode(method);
		this.variable.emitStore(method.node);
	}
}