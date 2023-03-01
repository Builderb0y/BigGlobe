package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class NoopInsnTree implements InsnTree {

	public static final NoopInsnTree INSTANCE = new NoopInsnTree();

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//nothing to do here.
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return nextStatement;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}