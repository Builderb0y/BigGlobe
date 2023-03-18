package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ScopedInsnTree implements InsnTree {

	public InsnTree body;

	public ScopedInsnTree(InsnTree body) {
		this.body = body;
	}

	public static InsnTree create(InsnTree body) {
		return new ScopedInsnTree(body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();
		this.body.emitBytecode(method.scopes.method);
		method.scopes.popScope();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.body.getTypeInfo();
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
	public boolean canBeStatement() {
		return this.body.canBeStatement();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree cast = this.body.cast(parser, type, mode);
		if (cast == null) return null;
		return new ScopedInsnTree(cast);
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		return new ScopedInsnTree(this.body.update(parser, op, rightValue));
	}
}