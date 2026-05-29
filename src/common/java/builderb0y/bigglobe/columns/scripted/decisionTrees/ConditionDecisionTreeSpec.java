package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConditionDecisionTreeSpec implements DecisionTreeSpec {

	public final ConditionProvider condition;
	public final Holder<DecisionTreeSpec> if_true, if_false;

	public ConditionDecisionTreeSpec(
		ConditionProvider condition,
		Holder<DecisionTreeSpec> if_true,
		Holder<DecisionTreeSpec> if_false
	) {
		this.condition = condition;
		this.if_true   = if_true;
		this.if_false  = if_false;
	}

	@Override
	public InsnTree emitTree(DecisionTreeContext context, Holder<DecisionTreeSpec> self) throws ScriptParsingException, ConstantFormatException {
		MethodCompileContext branch = context.newMethod("decision_tree_condition_", self, context.expectedTypeInfo());
		ifElse(
			CastingSupport.dummyParser(),
			this.condition.emitBoolean(
				context,
				self,
				Permuter.permute(0L, UnregisteredObjectException.getID(self))
			),
			return_(context.emitTree(this.if_true)),
			return_(context.emitTree(this.if_false))
		)
		.emitBytecode(branch);
		branch.endCode();
		return invokeInstance(context.loadColumn(), branch.info, context.yArguments());
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(this.condition.streamDirectDependencies(), Stream.of(this.if_true, this.if_false));
	}
}