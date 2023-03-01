package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class ReturnInsnTree implements InsnTree {

	public InsnTree value;

	public ReturnInsnTree(InsnTree value) {
		this.value = value;
	}

	public static InsnTree create(InsnTree value) {
		return value.returnsUnconditionally() ? value : new ReturnInsnTree(value);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
		method.node.visitInsn(this.value.getTypeInfo().getOpcode(IRETURN));
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean returnsUnconditionally() {
		return true;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}