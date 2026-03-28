package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.columns.scripted.decisionTrees.results.DecisionTreeResult;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ResultBasedDecisionTreeSettings extends DecisionTreeSettings {

	public final DecisionTreeResult result;

	public ResultBasedDecisionTreeSettings(DecisionTreeResult result) {
		this.result = result;
	}

	@Override
	public InsnTree doCreateResult(Holder<DecisionTreeSettings> selfEntry, Context context) throws ScriptParsingException {
		return this.result.createResult(selfEntry, context.dataContext, context.accessSchema, context.loadY);
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.result.streamDirectDependencies();
	}
}