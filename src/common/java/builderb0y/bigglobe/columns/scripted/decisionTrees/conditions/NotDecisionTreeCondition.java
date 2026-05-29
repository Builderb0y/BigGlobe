package builderb0y.bigglobe.columns.scripted.decisionTrees.conditions;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.columns.scripted2.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted2.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NotDecisionTreeCondition implements DecisionTreeCondition {

	public final DecisionTreeCondition condition;

	public NotDecisionTreeCondition(DecisionTreeCondition condition) {
		this.condition = condition;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.condition.streamDirectDependencies();
	}

	@Override
	public ConditionTree createCondition(Holder<DecisionTreeSettings> selfEntry, long selfSeed, DataCompileContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		return not(this.condition.createCondition(selfEntry, selfSeed, context, loadY));
	}
}