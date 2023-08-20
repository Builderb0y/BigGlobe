package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;

public class WrappedCastInsnTree implements InsnTree {

	public InsnTree compileValue, runtimeValue;

	public WrappedCastInsnTree(InsnTree compileValue, InsnTree runtimeValue) {
		this.compileValue = compileValue;
		this.runtimeValue = runtimeValue;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.runtimeValue.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.runtimeValue.getTypeInfo();
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.runtimeValue.jumpsUnconditionally();
	}

	@Override
	public ConstantValue getConstantValue() {
		return this.runtimeValue.getConstantValue();
	}

	@Override
	public boolean canBeStatement() {
		return this.runtimeValue.canBeStatement();
	}

	@Override
	public InsnTree asStatement() {
		return this.compileValue.asStatement();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		return this.compileValue.cast(parser, type, mode);
	}
}