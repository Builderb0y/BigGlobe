package builderb0y.bigglobe.columns.scripted.tree;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

//`example_mod:example_value`(y)
public class StandAloneDirect3DGetterInsnTree implements InsnTree {

	public final InsnTree column, y;
	public final MethodInfo getter;
	public final @Nullable MethodInfo setter;

	public StandAloneDirect3DGetterInsnTree(
		InsnTree column,
		InsnTree y,
		MethodInfo getter,
		@Nullable MethodInfo setter
	) {
		this.column = column;
		this.y = y;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		this.column.emitBytecode(method);
		this.y.emitBytecode(method);
		this.getter.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.getter.returnType;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (this.setter != null) {
			if (op == UpdateOp.ASSIGN) {
				InsnTree cast = rightValue.cast(parser, this.getter.returnType, CastMode.IMPLICIT_THROW);
				return new StandAloneDirect3DSetterInsnTree(CombinedMode.of(order, true), this.column, this.y, cast, this.getter, this.setter);
			}
			else {
				InsnTree updater = op.createUpdater(parser, this.getter.returnType, rightValue);
				return new StandAloneDirect3DSetterInsnTree(CombinedMode.of(order, false), this.column, this.y, updater, this.getter, this.setter);
			}
		}
		return InsnTree.super.update(parser, op, order, rightValue);
	}
}