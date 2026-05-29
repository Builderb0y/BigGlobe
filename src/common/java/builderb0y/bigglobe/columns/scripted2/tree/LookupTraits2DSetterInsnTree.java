package builderb0y.bigglobe.columns.scripted2.tree;

import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//world_traits.`example_mod:example_value`(x, z) = value
public class LookupTraits2DSetterInsnTree extends Abstract2DSetterInsnTree {

	public final InsnTree lookup, x, z;

	public LookupTraits2DSetterInsnTree(
		CombinedMode mode,
		InsnTree lookup,
		InsnTree x,
		InsnTree z,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode, updater, getter, setter);
		this.lookup = lookup;
		this.x = x;
		this.z = z;
	}

	@Override
	public void emitColumn(MethodCompileContext method) {
		this.lookup.emitBytecode(method);
		this.x.emitBytecode(method);
		this.z.emitBytecode(method);
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method);
	}

	@Override
	public void emitGet(MethodCompileContext method) {
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.returnType.getInternalName()); //column traits
		method.node.visitInsn(SWAP); //traits column
		this.getter.emitBytecode(method);
	}

	@Override
	public void emitSet(MethodCompileContext method) {
		method.node.visitInvokeDynamicInsn(
			"set",
			this.setter.getDescriptor(),
			BootstrapTraitsMethods.COLUMN_VALUE_SETTER_VIA_TRAITS.toHandle(H_INVOKESTATIC),
			this.setter.toHandle(H_INVOKEVIRTUAL)
		);
	}

	@Override
	public InsnTree asStatement(boolean force) {
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