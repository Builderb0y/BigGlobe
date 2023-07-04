package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

public class BlockInsnTree implements InsnTree {

	public String loopName;
	public InsnTree body;

	public BlockInsnTree(String loopName, InsnTree body) {
		this.loopName = loopName;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushLoop(this.loopName);
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
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		return new BlockInsnTree(this.loopName, this.body.doCast(parser, type, mode));
	}
}