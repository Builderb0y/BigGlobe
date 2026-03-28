package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//`example_mod:example_value`(x, z) = value
public class LookupDirect2DSetterInsnTree extends Abstract2DSetterInsnTree {

	public final InsnTree lookup, x, z;

	public LookupDirect2DSetterInsnTree(
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
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.z.emitBytecode(method); //lookup x z
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method); //column
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //column
	}

	@Override
	public void emitGet(MethodCompileContext method) {
		this.getter.emitBytecode(method);
	}

	@Override
	public void emitSet(MethodCompileContext method) {
		this.setter.emitBytecode(method);
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new LookupDirect2DSetterInsnTree(
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