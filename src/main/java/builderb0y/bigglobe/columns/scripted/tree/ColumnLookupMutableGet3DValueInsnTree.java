package builderb0y.bigglobe.columns.scripted.tree;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.AbstractUpdaterInsnTree.CombinedMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnLookupMutableGet3DValueInsnTree extends ColumnLookupGet3DValueInsnTree {

	public MethodInfo setter;

	public ColumnLookupMutableGet3DValueInsnTree(
		InsnTree lookup,
		InsnTree x,
		InsnTree y,
		InsnTree z,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(lookup, x, y, z, getter);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.getTypeInfo(), CastMode.IMPLICIT_THROW);
			return new ColumnLookupSet3DValueInsnTree(this.lookup, this.x, this.y, this.z, this.getter, this.setter, cast, CombinedMode.of(order, true));
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return new ColumnLookupSet3DValueInsnTree(this.lookup, this.x, this.y, this.z, this.getter, this.setter, updater, CombinedMode.of(order, false));
		}
	}
}