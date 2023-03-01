package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.InstanceGetterSetterUpdateInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class InstanceGetterInsnTree extends InvokeInsnTree {

	public MethodInfo setter;

	public InstanceGetterInsnTree(
		int opcode,
		InsnTree receiver,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(opcode, receiver, getter);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			return new InvokeInsnTree(this.opcode, this.receiver, this.setter, rightValue);
		}
		return new InstanceGetterSetterUpdateInsnTree(this.receiver, this.opcode, this.method, this.setter, op.createUpdater(parser, this.getTypeInfo(), rightValue));
	}
}