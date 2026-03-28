package builderb0y.scripting.bytecode.tree.instructions.casting;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

public class D2ZInsnTree implements InsnTree {

	public InsnTree value;

	public D2ZInsnTree(InsnTree value) {
		this.value = value;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.value.emitBytecode(method);
		method.node.visitInsn(DUP2);
		method.node.visitInsn(DCMPG);
		method.node.visitInsn(ICONST_1);
		method.node.visitInsn(IXOR);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}