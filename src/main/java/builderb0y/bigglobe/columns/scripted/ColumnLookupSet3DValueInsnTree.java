package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree;

public class ColumnLookupSet3DValueInsnTree extends AbstractUpdaterInsnTree {

	public static final MethodInfo LOOKUP_COLUMN = MethodInfo.getMethod(ScriptedColumnLookup.class, "lookupColumn");

	public InsnTree lookup, x, y, z, updater;
	public MethodInfo getter, setter;

	public ColumnLookupSet3DValueInsnTree(
		InsnTree lookup,
		InsnTree x,
		InsnTree y,
		InsnTree z,
		MethodInfo getter,
		MethodInfo setter,
		InsnTree updater,
		CombinedMode mode
	) {
		super(mode);
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
		this.getter = getter;
		this.setter = setter;
		this.updater = updater;
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
		//column y
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP2);
				this.getter.emitBytecode(method);
				this.updater.emitBytecode(method);
				this.setter.emitBytecode(method);
			}
			case PRE -> {
				method.node.visitInsn(DUP2);
				this.getter.emitBytecode(method);
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.updater.emitBytecode(method);
				this.setter.emitBytecode(method);
			}
			case POST -> {
				method.node.visitInsn(DUP2);
				this.getter.emitBytecode(method);
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.setter.emitBytecode(method);
			}
			case VOID_ASSIGN -> {
				this.updater.emitBytecode(method);
				this.setter.emitBytecode(method);
			}
			case PRE_ASSIGN -> {
				method.node.visitInsn(DUP2);
				this.getter.emitBytecode(method);
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2);
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? POP2 : POP);
				this.updater.emitBytecode(method);
				this.setter.emitBytecode(method);
			}
			case POST_ASSIGN -> {
				this.updater.emitBytecode(method);
				method.node.visitInsn(this.updater.getTypeInfo().isDoubleWidth() ? DUP2_X2 : DUP_X2);
				this.setter.emitBytecode(method);
			}
		}
	}

	@Override
	public TypeInfo getPreType() {
		return this.getter.returnType;
	}

	@Override
	public TypeInfo getPostType() {
		return this.updater.getTypeInfo();
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new ColumnLookupSet3DValueInsnTree(this.lookup, this.x, this.y, this.z, this.getter, this.setter,  this.updater, this.mode);
	}
}