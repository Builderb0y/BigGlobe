package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifySizeRange;
import builderb0y.bigglobe.columns.scripted.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AndDecisionTreeCondition implements DecisionTreeCondition {

	public final DecisionTreeCondition @VerifySizeRange(min = 2) [] conditions;

	public AndDecisionTreeCondition(DecisionTreeCondition... conditions) {
		this.conditions = conditions;
	}

	@Override
	public void addDependency(RegistryEntry<? extends ColumnValueDependencyHolder> entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<RegistryEntry<? extends ColumnValueDependencyHolder>> getDependencies() {
		return Arrays.stream(this.conditions).map(DecisionTreeCondition::getDependencies).flatMap(Set::stream).collect(Collectors.toSet());
	}

	@Override
	public ConditionTree createCondition(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		long selfSeed,
		DataCompileContext context,
		@Nullable InsnTree loadY
	)
	throws ScriptParsingException {
		DecisionTreeCondition[] conditions = this.conditions;
		int length = conditions.length;
		ConditionTree result = conditions[0].createCondition(selfEntry, Permuter.permute(selfSeed, 0), context, loadY);
		for (int index = 1; index < length; index++) {
			result = and(result, conditions[index].createCondition(selfEntry, Permuter.permute(selfSeed, index), context, loadY));
		}
		return result;
	}
}