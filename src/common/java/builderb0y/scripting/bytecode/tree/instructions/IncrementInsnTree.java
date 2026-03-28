package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class IncrementInsnTree implements InsnTree {

	public LazyVarInfo variable;
	public int amount;

	public IncrementInsnTree(LazyVarInfo variable, int amount) {
		this.variable = variable;
		this.amount = amount;
	}

	public static IncrementInsnTree create(LazyVarInfo variable, int amount) {
		if (variable.type.getSort() != Sort.INT) {
			throw new IllegalArgumentException("Can only increment ints");
		}
		return new IncrementInsnTree(variable, amount);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.node.visitIincInsn(method.scopes.getVariableIndex(this.variable), this.amount);
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