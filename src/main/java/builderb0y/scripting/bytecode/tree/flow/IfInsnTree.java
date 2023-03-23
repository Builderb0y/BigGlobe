package builderb0y.scripting.bytecode.tree.flow;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.util.TypeInfos;

public class IfInsnTree implements InsnTree {

	public final ConditionTree condition;
	public final InsnTree trueBody;

	public IfInsnTree(ConditionTree condition, InsnTree trueBody) {
		this.condition = condition;
		this.trueBody  = trueBody.asStatement();
	}

	public static InsnTree create(ConditionTree condition, InsnTree body) {
		return new IfInsnTree(condition, body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();
		this.condition.emitBytecode(method, null, method.scopes.peekScope().end.getLabel());
		this.trueBody.emitBytecode(method);
		method.scopes.popScope();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}