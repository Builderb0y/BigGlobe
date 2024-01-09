package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnGetterInsnTree3D extends BaseInvokeInsnTree {

	public MethodInfo setter;

	public ColumnGetterInsnTree3D(InsnTree accessor, InsnTree column, InsnTree y, MethodInfo getter, MethodInfo setter) {
		super(accessor, getter, column, y);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (order == UpdateOrder.VOID) {
			return new ColumnUpdateInsnTree3D(op == UpdateOp.ASSIGN, this.args[0], this.args[1], this.args[2], rightValue, this.method.returnType, this.method, this.setter);
		}
		else {
			throw new ScriptParsingException("For technical reasons, the updated value cannot be reused", parser.input);
		}
	}
}