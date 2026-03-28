package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//world_traits.`example_mod:example_value`(y) = value
public class StandAloneTraits3DSetterInsnTree extends Abstract3DSetterInsnTree {

	public final InsnTree column, y;

	public StandAloneTraits3DSetterInsnTree(
		CombinedMode mode,
		InsnTree column,
		InsnTree y,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode, updater, getter, setter);
		this.column = column;
		this.y = y;
	}

	@Override
	public void emitColumnY(MethodCompileContext method) {
		this.column.emitBytecode(method);
		this.y.emitBytecode(method);
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
		return this.mode.isVoid() ? this : new StandAloneTraits3DSetterInsnTree(
			this.mode.asVoid(),
			this.column,
			this.y,
			this.updater,
			this.getter,
			this.setter
		);
	}
}