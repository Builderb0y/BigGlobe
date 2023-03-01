package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.util.TypeInfos;

public class MethodDeclarationInsnTree implements InsnTree {

	public MethodCompileContext method;
	public InsnTree body;
	public VarInfo[] parameters;

	public MethodDeclarationInsnTree(MethodCompileContext method, InsnTree body, VarInfo[] parameters) {
		this.method = method;
		this.body = body;
		this.parameters = parameters;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.method.scopes.pushScope();
		if (!this.method.info.isStatic()) {
			this.method.addThis();
		}
		for (VarInfo parameter : this.parameters) {
			VarInfo added = this.method.newParameter(parameter.name, parameter.type);
			if (added.index != parameter.index) {
				throw new IllegalStateException("Parameter index mismatch: " + parameter + " -> " + added);
			}
		}

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