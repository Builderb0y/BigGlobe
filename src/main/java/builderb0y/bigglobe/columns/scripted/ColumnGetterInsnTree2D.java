package builderb0y.bigglobe.columns.scripted;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnGetterInsnTree2D extends BaseInvokeInsnTree {

	public MethodInfo setter;

	public ColumnGetterInsnTree2D(InsnTree accessor, InsnTree column, MethodInfo getter, MethodInfo setter) {
		super(accessor, getter, column);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (order == UpdateOrder.VOID) {
			return new ColumnUpdateInsnTree2D(op == UpdateOp.ASSIGN, this.args[0], this.args[1], rightValue, this.method.returnType, this.method, this.setter);
		}
		else {
			throw new ScriptParsingException("For technical reasons, the updated value cannot be reused", parser.input);
		}
	}
}