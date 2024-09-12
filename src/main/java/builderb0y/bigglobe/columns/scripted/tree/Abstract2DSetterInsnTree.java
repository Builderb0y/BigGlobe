package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree;

public abstract class Abstract2DSetterInsnTree extends AbstractUpdaterInsnTree {

	public final InsnTree updater;
	public final MethodInfo getter, setter;

	public Abstract2DSetterInsnTree(
		CombinedMode mode,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode);
		this.updater = updater;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.emitColumn(method); //column
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP); //column column
				this.emitGet(method); //column oldValue
				this.updater.emitBytecode(method); //column newValue
				this.emitSet(method); //
			}
			case PRE -> {
				method.node.visitInsn(DUP); //column column
				this.emitGet(method); //column oldValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X1
					: DUP_X1
				); //oldValue column oldValue
				this.updater.emitBytecode(method); //oldValue column newValue
				this.emitSet(method); //oldValue
			}
			case POST -> {
				method.node.visitInsn(DUP); //column column
				this.emitGet(method); //column oldValue
				this.updater.emitBytecode(method); //column newValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X1
					: DUP_X1
				); //newValue column newValue
				this.emitSet(method); //newValue
			}
			case VOID_ASSIGN -> {
				this.updater.emitBytecode(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				//column
				method.node.visitInsn(DUP); //column column
				this.emitGet(method); //column oldValue
				if (this.getter.returnType.isDoubleWidth()) {
					method.node.visitInsn(DUP2_X1);
					method.node.visitInsn(POP2);
				}
				else {
					method.node.visitInsn(SWAP);
				}
				//oldValue column
				this.updater.emitBytecode(method); //oldValue column newValue
				this.emitSet(method); //oldValue
			}
			case POST_ASSIGN -> {
				this.updater.emitBytecode(method); //column newValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X1
					: DUP_X1
				); //newValue column newValue
				this.emitSet(method); //newValue
			}
		}
	}

	public abstract void emitColumn(MethodCompileContext method);

	public abstract void emitGet(MethodCompileContext method);

	public abstract void emitSet(MethodCompileContext method);

	@Override
	public TypeInfo getPreType() {
		return this.getter.returnType;
	}

	@Override
	public TypeInfo getPostType() {
		return this.updater.getTypeInfo();
	}
}