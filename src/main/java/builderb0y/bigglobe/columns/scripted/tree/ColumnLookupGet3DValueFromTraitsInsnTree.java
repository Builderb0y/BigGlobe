package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ColumnLookupGet3DValueFromTraitsInsnTree implements InsnTree {

	public InsnTree lookup, x, y, z;
	public MethodInfo getter;

	public ColumnLookupGet3DValueFromTraitsInsnTree(InsnTree lookup, InsnTree x, InsnTree y, InsnTree z, MethodInfo getter) {
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
		this.getter = getter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.y.emitBytecode(method); //lookup x y
		method.node.visitInsn(DUP_X2); //y lookup x y
		method.node.visitInsn(POP); //y lookup x
		this.z.emitBytecode(method); //y lookup x z
		ColumnLookupGet3DValueInsnTree.LOOKUP_COLUMN.emitBytecode(method); //y column
		method.node.visitTypeInsn(CHECKCAST, this.getter.paramTypes[0].getInternalName());
		method.node.visitInsn(DUP); //y column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //y column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName());
		method.node.visitInsn(DUP_X2); //traits y column traits
		method.node.visitInsn(POP); //traits y column
		method.node.visitInsn(SWAP); //traits column y
		this.getter.emitBytecode(method); //value
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}
}