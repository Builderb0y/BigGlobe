package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.classes.BorderedValue;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BorderDecisionTreeSpec implements DecisionTreeSpec {

	public final BorderProvider border;
	public final Holder<DecisionTreeSpec> if_positive, if_negative;

	public BorderDecisionTreeSpec(
		BorderProvider border,
		Holder<DecisionTreeSpec> if_positive,
		Holder<DecisionTreeSpec> if_negative
	) {
		this.border = border;
		this.if_positive = if_positive;
		this.if_negative = if_negative;
	}

	@Override
	public InsnTree emitTree(DecisionTreeContext context, Holder<DecisionTreeSpec> self) throws ScriptParsingException, ConstantFormatException {
		if (!context.hasBorder) throw new IllegalArgumentException(
			"Decision tree " +
			UnregisteredObjectException.getID(self) +
			" has a border, but was used by column value " +
			UnregisteredObjectException.getID(context.columnEntry) +
			", which does not have a border."
		);
		MethodCompileContext branch = context.newMethod("decision_tree_border_", self, TypeInfos.VOID, new LazyVarInfo("border", BorderedValue.TYPE));
		InsnTree value = this.border.emitBorder(context, self);
		InsnTree test = invokeInstance(load("border", BorderedValue.TYPE), BorderedValue.APPLY_BORDER, value);
		InsnTree ifPositive = context.emitTree(this.if_positive);
		InsnTree ifNegative = context.emitTree(this.if_negative);
		ifElse(
			CastingSupport.dummyParser(),
			condition(CastingSupport.dummyParser(), test),
			return_(ifPositive),
			return_(ifNegative)
		)
		.emitBytecode(branch);
		branch.endCode();
		return invokeInstance(context.loadColumn(), branch.info, context.yArgumentsWith(context.is3D, load("border", BorderedValue.TYPE)));
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(this.border.streamDirectDependencies(), Stream.of(this.if_positive, this.if_negative));
	}
}