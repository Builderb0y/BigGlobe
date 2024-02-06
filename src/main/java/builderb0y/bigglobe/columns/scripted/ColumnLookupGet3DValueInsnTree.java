package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ColumnLookupGet3DValueInsnTree implements InsnTree {

	public static final MethodInfo LOOKUP_COLUMN = MethodInfo.getMethod(ScriptedColumnLookup.class, "lookupColumn");

	public InsnTree lookup, x, y, z;
	public MethodInfo getter;

	public ColumnLookupGet3DValueInsnTree(InsnTree lookup, InsnTree x, InsnTree y, InsnTree z, MethodInfo getter) {
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
		this.getter = getter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.lookup.emitBytecode(method);
		this.x.emitBytecode(method);
		this.y.emitBytecode(method);
		method.node.visitInsn(DUP_X2);
		method.node.visitInsn(POP);
		this.z.emitBytecode(method);
		LOOKUP_COLUMN.emitBytecode(method);
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName());
		method.node.visitInsn(SWAP);
		this.getter.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}
}