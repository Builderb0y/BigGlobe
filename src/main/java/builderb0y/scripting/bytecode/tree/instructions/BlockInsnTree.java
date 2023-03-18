package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

public class BlockInsnTree implements InsnTree {

	public InsnTree body;

	public BlockInsnTree(InsnTree body) {
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushLoop();
		this.body.emitBytecode(method);
		method.scopes.popLoop();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.body.getTypeInfo();
	}

	@Override
	public boolean canBeStatement() {
		return this.body.canBeStatement();
	}

	@Override
	public ConstantValue getConstantValue() {
		return this.body.getConstantValue();
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.body.jumpsUnconditionally();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		return new BlockInsnTree(this.body.doCast(parser, type, mode));
	}
}