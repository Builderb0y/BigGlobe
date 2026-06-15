package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.BorderedValue;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ResultDecisionTreeSpec implements DecisionTreeSpec {

	public final ResultProvider result;

	public ResultDecisionTreeSpec(ResultProvider result) {
		this.result = result;
	}

	@Override
	public InsnTree emitTree(DecisionTreeContext context, Holder<DecisionTreeSpec> self) throws ScriptParsingException, ConstantFormatException {
		InsnTree result = this.result.emitValue(context, self, Permuter.permute(0L, UnregisteredObjectException.getID(self)));
		if (context.hasBorder) {
			return invokeInstance(load("border", BorderedValue.TYPE), BorderedValue.APPLY_VALUE, result);
		}
		else {
			return result;
		}
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.result.streamDirectDependencies();
	}
}