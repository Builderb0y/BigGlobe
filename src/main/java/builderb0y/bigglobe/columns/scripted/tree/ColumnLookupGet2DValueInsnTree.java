package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ColumnLookupGet2DValueInsnTree implements InsnTree {

	public InsnTree loadLookup, x, z;
	public MethodInfo getter;

	public ColumnLookupGet2DValueInsnTree(InsnTree loadLookup, InsnTree x, InsnTree z, MethodInfo getter) {
		this.loadLookup = loadLookup;
		this.x = x;
		this.z = z;
		this.getter = getter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.loadLookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.z.emitBytecode(method); //lookup x z
		ColumnLookupGet3DValueInsnTree.LOOKUP_COLUMN.emitBytecode(method); //column
		method.node.visitTypeInsn(CHECKCAST, this.getter.paramTypes[0].getInternalName());
		this.getter.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}
}