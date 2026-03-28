package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.columns.scripted.decisionTrees.conditions.DecisionTreeCondition;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ConditionBasedDecisionTreeSettings extends DecisionTreeSettings {

	public final DecisionTreeCondition condition;
	public final Holder<DecisionTreeSettings> if_true, if_false;

	public ConditionBasedDecisionTreeSettings(
		DecisionTreeCondition condition,
		Holder<DecisionTreeSettings> if_true,
		Holder<DecisionTreeSettings> if_false
	) {
		this.condition = condition;
		this.if_true = if_true;
		this.if_false = if_false;
	}

	@Override
	public InsnTree doCreateResult(Holder<DecisionTreeSettings> selfEntry, Context context) throws ScriptParsingException {
		ConditionTree condition = this.condition.createCondition(selfEntry, Permuter.permute(0L, UnregisteredObjectException.getID(selfEntry)), context.dataContext, context.loadY);
		InsnTree ifTrue = context.createInsnTree(this.if_true);
		InsnTree ifFalse = context.createInsnTree(this.if_false);
		if (!ifTrue.getTypeInfo().equals(ifFalse.getTypeInfo())) {
			throw new DecisionTreeException(UnregisteredObjectException.getKey(this.if_true) + " and " + UnregisteredObjectException.getKey(this.if_false) + " do not have the same return type.");
		}
		return new IfElseInsnTree(condition, ifTrue, ifFalse, ifTrue.getTypeInfo());
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(Stream.of(this.if_true, this.if_false), this.condition.streamDirectDependencies());
	}
}