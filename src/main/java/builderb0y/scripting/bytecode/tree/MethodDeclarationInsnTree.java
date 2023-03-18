package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.TypeInfos;

/**
part of method declaration logic needs to be delayed in
order for local variable capturing to work correctly.
specifically, the method needs to be converted to
bytecode after {@link VariableDeclarationInsnTree}
updates the variable's index.
*/
public class MethodDeclarationInsnTree implements InsnTree {

	public MethodCompileContext method;
	public InsnTree body;

	public MethodDeclarationInsnTree(MethodCompileContext method, InsnTree body) {
		this.method = method;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.body.emitBytecode(this.method);
		this.method.scopes.popScope();
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}
}