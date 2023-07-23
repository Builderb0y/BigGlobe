package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.VarInfo;

public class VariableDeclarePostAssignInsnTree extends VariableDeclarationInsnTree {

	public InsnTree initializer;

	public VariableDeclarePostAssignInsnTree(String name, TypeInfo type, InsnTree initializer) {
		super(name, type);
		this.initializer = initializer;
	}

	public VariableDeclarePostAssignInsnTree(VarInfo variable, InsnTree initializer) {
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