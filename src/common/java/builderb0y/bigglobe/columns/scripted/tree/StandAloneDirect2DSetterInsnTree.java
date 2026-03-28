package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//`example_mod:example_value` = value
public class StandAloneDirect2DSetterInsnTree extends Abstract2DSetterInsnTree {

	public final InsnTree column;

	public StandAloneDirect2DSetterInsnTree(
		CombinedMode mode,
		InsnTree column,
		InsnTree updater,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(mode, updater, getter, setter);
		this.column = column;
	}

	@Override
	public void emitColumn(MethodCompileContext method) {
		this.column.emitBytecode(method);
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
		return this.mode.isVoid() ? this : new StandAloneDirect2DSetterInsnTree(
			this.mode.asVoid(),
			this.column,
			this.updater,
			this.getter,
			this.setter
		);
	}
}