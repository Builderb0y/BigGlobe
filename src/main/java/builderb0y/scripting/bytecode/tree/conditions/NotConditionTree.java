package builderb0y.scripting.bytecode.tree.conditions;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;

public class NotConditionTree implements ConditionTree {

	public final ConditionTree condition;

	public NotConditionTree(ConditionTree condition) {
		this.condition = condition;
	}

	public static ConditionTree create(ConditionTree condition) {
		if (condition instanceof NotConditionTree not) {
			return not.condition;
		}
		else {
			return new NotConditionTree(condition);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method, @Nullable Label ifTrue, @Nullable Label ifFalse) {
		ConditionTree.checkLabels(ifTrue, ifFalse);
		this.condition.emitBytecode(method, ifFalse, ifTrue);
	}
}