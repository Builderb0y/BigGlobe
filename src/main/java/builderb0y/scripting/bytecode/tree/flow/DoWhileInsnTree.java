package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class DoWhileInsnTree implements InsnTree {

	public final ConditionTree condition;
	public final InsnTree body;

	public DoWhileInsnTree(ExpressionParser parser, ConditionTree condition, InsnTree body) {
		this.condition = condition;
		if (!body.canBeStatement()) {
			throw new IllegalArgumentException("Body is not a statement");
		}
		this.body = body.cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label start = new Label();
		method.node.visitLabel(start);
		this.body.emitBytecode(method);
		this.condition.emitBytecode(method, start, null);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean returnsUnconditionally() {
		//while (true) doesn't need a return after it.
		return this.condition instanceof ConstantConditionTree constant && constant.value;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}