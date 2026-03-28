package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

public class VariableDeclarationInsnTree implements InsnTree {

	public LazyVarInfo variable;

	public VariableDeclarationInsnTree(LazyVarInfo variable) {
		this.variable = variable;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.addVariable(this.variable);
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