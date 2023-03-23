package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.util.TypeInfos;

public class VariableDeclarationInsnTree implements InsnTree {

	public LoadInsnTree loader;

	public VariableDeclarationInsnTree(String name, TypeInfo type) {
		this.loader = new LoadInsnTree(new VarInfo(name, -1, type));
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		VarInfo variable = this.loader.variable;
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