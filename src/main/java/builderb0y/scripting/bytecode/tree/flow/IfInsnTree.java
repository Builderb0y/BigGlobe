package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class IfInsnTree implements InsnTree {

	public final ConditionTree condition;
	public final InsnTree trueBody;

	public IfInsnTree(ExpressionParser parser, ConditionTree condition, InsnTree trueBody) {
		this.condition = condition;
		if (!trueBody.canBeStatement()) {
			throw new IllegalArgumentException("Body is not a statement");
		}
		this.trueBody = trueBody.cast(parser, TypeInfos.VOID, CastMode.EXPLICIT_THROW);
	}

	public static InsnTree create(ExpressionParser parser, ConditionTree condition, InsnTree body) {
		return new IfInsnTree(parser, condition, body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label end = new Label();
		this.condition.emitBytecode(method, null, end);
		this.trueBody.emitBytecode(method);
		method.node.visitLabel(end);
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