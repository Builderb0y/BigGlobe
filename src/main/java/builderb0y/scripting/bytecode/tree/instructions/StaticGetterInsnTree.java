package builderb0y.scripting.bytecode.tree.instructions;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.StaticGetterSetterUpdateInsnTree.*;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class StaticGetterInsnTree extends InvokeStaticInsnTree {

	public MethodInfo setter;

	public StaticGetterInsnTree(MethodInfo getter, MethodInfo setter, InsnTree... args) {
		super(getter, args);
		this.setter = setter;
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return switch (order) {
				case VOID -> new StaticGetterSetterAssignVoidUpdateInsnTree(this.method, this.setter, cast);
				case PRE  -> new  StaticGetterSetterAssignPreUpdateInsnTree(this.method, this.setter, cast);
				case POST -> new StaticGetterSetterAssignPostUpdateInsnTree(this.method, this.setter, cast);
			};
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.getTypeInfo(), rightValue);
			return switch (order) {
				case VOID -> new StaticGetterSetterVoidUpdateInsnTree(this.method, this.setter, updater);
				case PRE  -> new  StaticGetterSetterPreUpdateInsnTree(this.method, this.setter, updater);
				case POST -> new StaticGetterSetterPostUpdateInsnTree(this.method, this.setter, updater);
			};
		}
	}
}