package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.UnaryInsnTree;

public class IdentityCastInsnTree extends UnaryInsnTree {

	public TypeInfo type;

	public IdentityCastInsnTree(InsnTree operand, TypeInfo type) {
		super(operand);
		this.type = type;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return this.operand.jumpsUnconditionally();
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.operand.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public boolean canBeStatement() {
		return this.operand.canBeStatement();
	}

	@Override
	public InsnTree asStatement() {
		return this.operand.asStatement();
	}
}