package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//world_traits.`example_mod:example_value`(x, y, z) = value
public class LookupTraits3DSetterInsnTree extends Abstract3DSetterInsnTree {

	public final InsnTree lookup, x, y, z;

	public LookupTraits3DSetterInsnTree(
		CombinedMode mode,
		InsnTree lookup,
		InsnTree x,
		InsnTree y,
		InsnTree z,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode, updater, getter, setter);
		this.lookup = lookup;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void emitColumnY(MethodCompileContext method) {
		this.lookup.emitBytecode(method); //lookup
		this.x.emitBytecode(method); //lookup x
		this.y.emitBytecode(method); //lookup x y
		method.node.visitInsn(DUP_X2); //y lookup x y
		method.node.visitInsn(POP); //y lookup x
		this.z.emitBytecode(method); //y lookup x z
		ScriptedColumnLookup.LOOKUP_COLUMN.emitBytecode(method); //y column
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //y column
		method.node.visitInsn(SWAP); //column y
	}

	@Override
	public void emitGet(MethodCompileContext method) {
		method.node.visitInvokeDynamicInsn(
			"get",
			this.getter.getDescriptor(),
			BootstrapTraitsMethods.COLUMN_Y_TO_VALUE_VIA_TRAITS.toHandle(H_INVOKESTATIC),
			this.getter.toHandle(H_INVOKEVIRTUAL)
		);
	}

	@Override
	public void emitSet(MethodCompileContext method) {
		method.node.visitInvokeDynamicInsn(
			"set",
			this.setter.getDescriptor(),
			BootstrapTraitsMethods.COLUMN_Y_VALUE_SETTER_VIA_TRAITS.toHandle(H_INVOKESTATIC),
			this.setter.toHandle(H_INVOKEVIRTUAL)
		);
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new LookupTraits3DSetterInsnTree(
			this.mode.asVoid(),
			this.lookup,
			this.x,
			this.y,
			this.z,
			this.updater,
			this.getter,
			this.setter
		);
	}
}