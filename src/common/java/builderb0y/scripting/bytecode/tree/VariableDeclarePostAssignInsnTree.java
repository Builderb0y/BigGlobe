package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;

public class VariableDeclarePostAssignInsnTree extends VariableDeclarationInsnTree {

	public InsnTree initializer;

	public VariableDeclarePostAssignInsnTree(LazyVarInfo variable, InsnTree initializer) {
		super(variable);
		this.initializer = initializer;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		super.emitBytecode(method);
		this.initializer.emitBytecode(method);
		method.node.visitInsn(this.variable.type.isDoubleWidth() ? DUP2 : DUP);
		this.variable.emitStore(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.initializer.getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return new VariableDeclareAssignInsnTree(this.variable, this.initializer);
	}
}