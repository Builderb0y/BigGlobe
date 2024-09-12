package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;

//world_traits.`example_mod:example_value` = value
public class StandAloneTraits2DSetterInsnTree extends Abstract2DSetterInsnTree {

	public final InsnTree column;

	public StandAloneTraits2DSetterInsnTree(
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
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //column traits
		method.node.visitInsn(SWAP);
		this.getter.emitBytecode(method);
	}

	@Override
	public void emitSet(MethodCompileContext method) {
		method.node.visitInsn(DUP); //column column
		ScriptedColumn.INFO.worldTraits.emitBytecode(method); //column traits
		method.node.visitTypeInsn(CHECKCAST, this.getter.owner.getInternalName()); //column traits
		method.node.visitInsn(SWAP);
		this.setter.emitBytecode(method);
	}

	@Override
	public InsnTree asStatement() {
		return this.mode.isVoid() ? this : new StandAloneTraits2DSetterInsnTree(
			this.mode.asVoid(),
			this.column,
			this.updater,
			this.getter,
			this.setter
		);
	}
}