package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

@Deprecated //replaced by TypeInfo.generic.
public class AutomaticCastInsnTree implements InsnTree {

	public InsnTree value;

	public AutomaticCastInsnTree(InsnTree value) {
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.value.getTypeInfo();
	}

	@Override
	public ConstantValue getConstantValue() {
		return this.value.getConstantValue();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree tree = this.value.doCast(parser, type, mode.toExplicit());
		if (tree == null) return null;
		return new AutomaticCastInsnTree(tree);
	}

	@Override
	public InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return this.value.then(parser, nextStatement);
	}

	@Override
	public boolean returnsUnconditionally() {
		return this.value.returnsUnconditionally();
	}

	@Override
	public boolean canBeStatement() {
		return this.value.canBeStatement();
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		return this.value.update(parser, op, rightValue);
	}
}