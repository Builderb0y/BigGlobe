package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class F2ZInsnTree implements InsnTree {

	public InsnTree value;

	public F2ZInsnTree(InsnTree value) {
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
		method.node.visitInsn(DUP);
		method.node.visitInsn(FCMPG);
		method.node.visitInsn(ICONST_1);
		method.node.visitInsn(IXOR);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}