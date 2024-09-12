package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree;

public abstract class Abstract3DSetterInsnTree extends AbstractUpdaterInsnTree {

	public final InsnTree updater;
	public final MethodInfo getter, setter;

	public Abstract3DSetterInsnTree(
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
		this.emitColumnY(method);
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP2); //column y column y
				this.emitGet(method); //column y oldValue
				this.updater.emitBytecode(method); //column y newValue
				this.emitSet(method); //
			}
			case PRE -> {
				method.node.visitInsn(DUP2); //column y column y
				this.emitGet(method); //column y oldValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X2
					: DUP_X2
				); //oldValue column y oldValue
				this.updater.emitBytecode(method); //oldValue column y newValue
				this.emitSet(method); //oldValue
			}
			case POST -> {
				method.node.visitInsn(DUP2); //column y column y
				this.emitGet(method); //column y oldValue
				this.updater.emitBytecode(method); //column y newValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X2
					: DUP_X2
				); //newValue column y newValue
				this.emitSet(method); //newValue
			}
			case VOID_ASSIGN -> {
				this.updater.emitBytecode(method);
				this.emitSet(method);
			}
			case PRE_ASSIGN -> {
				//column y
				method.node.visitInsn(DUP2); //column y column y
				this.emitGet(method); //column y oldValue
				if (this.getter.returnType.isDoubleWidth()) {
					method.node.visitInsn(DUP2_X2);
					method.node.visitInsn(POP2);
				}
				else {
					method.node.visitInsn(DUP_X2);
					method.node.visitInsn(POP);
				}
				//oldValue column y
				this.updater.emitBytecode(method); //oldValue column y newValue
				this.emitSet(method); //oldValue
			}
			case POST_ASSIGN -> {
				//column y
				this.updater.emitBytecode(method); //column y newValue
				method.node.visitInsn(
					this.getter.returnType.isDoubleWidth()
					? DUP2_X2
					: DUP_X2
				); //newValue column y newValue
				this.emitSet(method); //newValue
			}
		}
	}

	public abstract void emitColumnY(MethodCompileContext method);

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