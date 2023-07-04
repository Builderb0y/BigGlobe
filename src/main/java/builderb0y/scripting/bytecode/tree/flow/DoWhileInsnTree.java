package builderb0y.scripting.bytecode.tree.flow;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class DoWhileInsnTree implements InsnTree {

	public String loopName;
	public ConditionTree condition;
	public InsnTree body;

	public DoWhileInsnTree(ExpressionParser parser, String loopName, ConditionTree condition, InsnTree body) {
		this.loopName = loopName;
		this.condition = condition;
		if (!body.canBeStatement()) {
			throw new IllegalArgumentException("Body is not a statement");
		}
		this.body = body.cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Scope scope = method.scopes.pushLoop(this.loopName);
		this.body.emitBytecode(method);
		this.condition.emitBytecode(method, scope.start.getLabel(), null);
		method.scopes.popLoop();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean jumpsUnconditionally() {
		//while (true) doesn't need a return after it.
		return this.condition instanceof ConstantConditionTree constant && constant.value;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}