package builderb0y.scripting.bytecode.tree.conditions;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.ConditionToBooleanInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public class BooleanToConditionTree extends IntCompareZeroConditionTree {

	public BooleanToConditionTree(InsnTree condition) {
		super(condition, IFNE);
	}

	public static ConditionTree create(ExpressionParser parser, InsnTree bool) {
		if (bool instanceof ConditionToBooleanInsnTree converter) {
			return converter.condition;
		}
		else {
			bool = bool.cast(parser, TypeInfos.BOOLEAN, CastMode.IMPLICIT_THROW);
			ConstantValue constant = bool.getConstantValue();
			if (constant.isConstant()) {
				return ConstantConditionTree.of(constant.asBoolean());
			}
			return new BooleanToConditionTree(bool);
		}
	}
}