package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

public class ColumnLookup3DValueInsnTree implements InsnTree {

	public static final MethodInfo LOOKUP_COLUMN = MethodInfo.getMethod(ScriptedColumnLookup.class, "lookupColumn");

	public InsnTree lookup, x, y, z;
	public MethodInfo _3DGetter;

	public ColumnLookup3DValueInsnTree(InsnTree lookup, InsnTree x, InsnTree y, InsnTree z, MethodInfo _3DGetter) {
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
		this._3DGetter = _3DGetter;
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
		method.node.visitTypeInsn(CHECKCAST, this._3DGetter.owner.getInternalName());
		method.node.visitInsn(SWAP);
		this._3DGetter.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this._3DGetter.returnType;
	}
}