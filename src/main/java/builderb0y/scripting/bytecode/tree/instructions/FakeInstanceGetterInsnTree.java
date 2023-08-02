package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.InvokeStaticInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.InstanceGetterSetterUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class FakeInstanceGetterInsnTree extends InvokeStaticInsnTree {

	public MethodInfo setter;

	public FakeInstanceGetterInsnTree(MethodInfo getter, MethodInfo setter, InsnTree receiver) {
		super(getter, receiver);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		InsnTree receiver = this.args[0];
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new InstanceGetterSetterAssignVoidUpdateInsnTree(receiver, this.method, this.setter, cast);
				case PRE  -> new  InstanceGetterSetterAssignPreUpdateInsnTree(receiver, this.method, this.setter, cast);
				case POST -> new InstanceGetterSetterAssignPostUpdateInsnTree(receiver, this.method, this.setter, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new InstanceGetterSetterVoidUpdateInsnTree(receiver, this.method, this.setter, updater);
				case PRE  -> new  InstanceGetterSetterPreUpdateInsnTree(receiver, this.method, this.setter, updater);
				case POST -> new InstanceGetterSetterPostUpdateInsnTree(receiver, this.method, this.setter, updater);
			};
		}
	}
}