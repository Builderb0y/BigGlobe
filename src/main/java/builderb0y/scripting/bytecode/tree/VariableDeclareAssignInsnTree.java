package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;

public class VariableDeclareAssignInsnTree extends VariableDeclarationInsnTree {

	public InsnTree initializer;

	public VariableDeclareAssignInsnTree(String name, TypeInfo type, InsnTree initializer) {
		super(name, type);
		this.initializer = initializer;
	}

	public VariableDeclareAssignInsnTree(VarInfo variable, InsnTree initializer) {
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