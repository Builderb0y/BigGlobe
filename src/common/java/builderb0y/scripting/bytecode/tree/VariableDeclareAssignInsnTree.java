package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;

public class VariableDeclareAssignInsnTree extends VariableDeclarationInsnTree {

	public InsnTree initializer;

	public VariableDeclareAssignInsnTree(LazyVarInfo variable, InsnTree initializer) {
		super(variable);
		this.initializer = initializer;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		super.emitBytecode(method);
		this.initializer.emitBytecode(method);
		this.variable.emitStore(method);
	}
}