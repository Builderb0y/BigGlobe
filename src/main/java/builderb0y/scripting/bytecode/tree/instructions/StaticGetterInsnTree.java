package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.StaticGetterSetterUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class StaticGetterInsnTree extends InvokeStaticInsnTree {

	public MethodInfo setter;

	public StaticGetterInsnTree(MethodInfo getter, MethodInfo setter, InsnTree... args) {
		super(getter, args);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			return new InvokeStaticInsnTree(this.setter, rightValue);
		}
		return new StaticGetterSetterUpdateInsnTree(this.method, this.setter, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}
}