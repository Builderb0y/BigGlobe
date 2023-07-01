package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.InstanceGetterSetterUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class InstanceGetterInsnTree extends InvokeInsnTree {

	public MethodInfo setter;

	public InstanceGetterInsnTree(
		InsnTree receiver,
		MethodInfo getter,
		MethodInfo setter
	) {
		super(receiver, getter);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new InstanceGetterSetterAssignVoidUpdateInsnTree(this.receiver, this.method, this.setter, cast);
				case PRE  -> new  InstanceGetterSetterAssignPreUpdateInsnTree(this.receiver, this.method, this.setter, cast);
				case POST -> new InstanceGetterSetterAssignPostUpdateInsnTree(this.receiver, this.method, this.setter, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new InstanceGetterSetterVoidUpdateInsnTree(this.receiver, this.method, this.setter, updater);
				case PRE  -> new  InstanceGetterSetterPreUpdateInsnTree(this.receiver, this.method, this.setter, updater);
				case POST -> new InstanceGetterSetterPostUpdateInsnTree(this.receiver, this.method, this.setter, updater);
			};
		}
	}
}