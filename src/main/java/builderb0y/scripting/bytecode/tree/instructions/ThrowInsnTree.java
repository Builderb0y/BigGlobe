package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.util.TypeInfos;

public class ThrowInsnTree implements InsnTree {

	public InsnTree value;

	public ThrowInsnTree(InsnTree value) {
		this.value = value;
	}

	public static InsnTree create(InsnTree value) {
		if (value.jumpsUnconditionally()) return value;
		if (!value.getTypeInfo().extendsOrImplements(TypeInfos.THROWABLE)) {
			throw new InvalidOperandException(value.getTypeInfo() + " does not extend java/lang/Throwable");
		}
		return new ThrowInsnTree(value);
	}

		@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
		method.node.visitInsn(ATHROW);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public boolean jumpsUnconditionally() {
		return true;
	}
}