package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update2.AbstractObjectUpdaterInsnTree.ObjectUpdaterEmitters;
import builderb0y.scripting.bytecode.tree.instructions.update2.NullableReceiverObjectUpdaterInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class NullableReceiverGetterSetterInsnTree extends NullableReceiverInvokeInsnTree {

	public MethodInfo setter;

	public NullableReceiverGetterSetterInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter) {
		super(receiver, getter);
		this.setter = setter;
		checkGetterSetter(receiver, getter, setter);
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return new NullableReceiverObjectUpdaterInsnTree(order, true, ObjectUpdaterEmitters.forGetterSetter(this.args[0], this.method, this.setter, cast));
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.method.returnType, rightValue);
			return new NullableReceiverObjectUpdaterInsnTree(order, false, ObjectUpdaterEmitters.forGetterSetter(this.args[0], this.method, this.setter, updater));
		}
	}
}