package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//`example_mod:example_value`(y) = value
public class StandAloneDirect3DSetterInsnTree extends Abstract3DSetterInsnTree {

	public final InsnTree column, y;

	public StandAloneDirect3DSetterInsnTree(
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
		this.getter.emitBytecode(method);
	}

	@Override
	public void emitSet(MethodCompileContext method) {
		this.setter.emitBytecode(method);
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new StandAloneDirect3DSetterInsnTree(
			this.mode.asVoid(),
			this.column,
			this.y,
			this.updater,
			this.getter,
			this.setter
		);
	}
}