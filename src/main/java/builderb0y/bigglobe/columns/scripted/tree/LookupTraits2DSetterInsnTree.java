package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree;

//world_traits.`example_mod:example_value`(x, z) = value
public class LookupTraits2DSetterInsnTree extends AbstractUpdaterInsnTree {

	public final InsnTree lookup, x, z, updater;
	public final MethodInfo getter, setter;

	public LookupTraits2DSetterInsnTree(
		CombinedMode mode,
		InsnTree lookup,
		InsnTree x,
		InsnTree z,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode);
		this.lookup = lookup;
		this.x = x;
		this.z = z;
		this.updater = updater;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.z.emitBytecode(method); //lookup x z
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method); //column
		method.node.visitTypeInsn(CHECKCAST, this.getter.paramTypes[0].getInternalName()); //column
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.returnType.getInternalName()); //column traits
		method.node.visitInsn(SWAP); //traits column
		switch (this.mode) {
			case VOID -> {
				method.node.visitInsn(DUP2); //traits column traits column
				this.getter.emitBytecode(method); //traits column oldValue
				this.updater.emitBytecode(method); //traits column newValue
				this.setter.emitBytecode(method); //
			}
			case PRE -> {
				method.node.visitInsn(DUP2); //traits column traits column
				this.getter.emitBytecode(method); //traits column oldValue
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2); //oldValue traits column oldValue
				this.updater.emitBytecode(method); //oldValue traits column newValue
				this.setter.emitBytecode(method); //oldValue
			}
			case POST -> {
				method.node.visitInsn(DUP2); //traits column traits column
				this.getter.emitBytecode(method); //traits column oldValue
				this.updater.emitBytecode(method); //traits column newValue
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2); //newValue traits column newValue
				this.setter.emitBytecode(method); //newValue
			}
			case VOID_ASSIGN -> {
				this.updater.emitBytecode(method);
				this.setter.emitBytecode(method);
			}
			case PRE_ASSIGN -> {
				method.node.visitInsn(DUP2); //traits column traits column
				this.getter.emitBytecode(method); //traits column oldValue
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2); //oldValue traits column oldValue
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? POP2 : POP); //oldValue traits column
				this.updater.emitBytecode(method); //oldValue traits column newValue
				this.setter.emitBytecode(method); //oldValue
			}
			case POST_ASSIGN -> {
				this.updater.emitBytecode(method); //traits column newValue
				method.node.visitInsn(this.getter.returnType.isDoubleWidth() ? DUP2_X2 : DUP_X2); //newValue traits column newValue
				this.setter.emitBytecode(method); //newValue
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
		return this.mode.isVoid() ? this : new LookupTraits2DSetterInsnTree(
			this.mode.asVoid(),
			this.lookup,
			this.x,
			this.z,
			this.updater,
			this.getter,
			this.setter
		);
	}
}