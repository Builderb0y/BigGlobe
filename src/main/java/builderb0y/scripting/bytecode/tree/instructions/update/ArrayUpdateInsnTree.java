package builderb0y.scripting.bytecode.tree.instructions.update;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ArrayUpdateInsnTree extends UpdateInsnTree {

	public InsnTree array;
	public InsnTree index;
	public TypeInfo componentType;

	public ArrayUpdateInsnTree(InsnTree array, InsnTree index, InsnTree updater) {
		super(updater);
		this.array = array;
		this.index = index;
		this.componentType = array.getTypeInfo().componentType;
		if (this.componentType == null) {
			throw new IllegalArgumentException("Not an array: " + array.getTypeInfo());
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.array.emitBytecode(method);
		this.index.emitBytecode(method);
		method.node.visitInsn(DUP2);
		method.node.visitInsn(this.componentType.getOpcode(IALOAD));
		this.updater.emitBytecode(method);
		method.node.visitInsn(this.componentType.getOpcode(IASTORE));
	}
}