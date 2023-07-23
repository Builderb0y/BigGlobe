package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.util.TypeInfos;

public class VariableDeclarationInsnTree implements InsnTree {

	public VarInfo variable;

	public VariableDeclarationInsnTree(String name, TypeInfo type) {
		this.variable = new VarInfo(name, -1, type);
	}

	public VariableDeclarationInsnTree(VarInfo variable) {
		this.variable = variable;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		VarInfo variable = this.variable;
		variable.index = method.newVariable(variable.name, variable.type).index;
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