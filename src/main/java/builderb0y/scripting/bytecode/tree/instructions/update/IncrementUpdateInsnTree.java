package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class IncrementUpdateInsnTree implements UpdateInsnTree {

	public UpdateOrder order;
	public LazyVarInfo variable;
	public int amount;

	public IncrementUpdateInsnTree(UpdateOrder order, LazyVarInfo variable, int amount) {
		this.order = order;
		this.variable = variable;
		this.amount = amount;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		int index = method.scopes.getVariableIndex(this.variable);
		switch (this.order) {
			case VOID -> {
				method.node.visitIincInsn(index, this.amount);
			}
			case PRE -> {
				this.variable.emitLoad(method);
				method.node.visitIincInsn(index, this.amount);
			}
			case POST -> {
				method.node.visitIincInsn(index, this.amount);
				this.variable.emitLoad(method);
			}
		}
	}

	@Override
	public TypeInfo getTypeInfo() {
		return switch (this.order) {
			case VOID -> TypeInfos.VOID;
			case PRE, POST -> TypeInfos.INT;
		};
	}

	@Override
	public TypeInfo getPreType() {
		return TypeInfos.INT;
	}

	@Override
	public TypeInfo getPostType() {
		return TypeInfos.INT;
	}

	@Override
	public InsnTree asStatement() {
		return this.order == UpdateOrder.VOID ? this : new IncrementUpdateInsnTree(UpdateOrder.VOID, this.variable, this.amount);
	}
}