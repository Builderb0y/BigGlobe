package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ScriptParsingException;

@UseCoder(name = "REGISTRY", in = DecisionTreeCondition.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface DecisionTreeCondition extends CoderRegistryTyped<DecisionTreeCondition>, ColumnValueDependencyHolder {

	public static final CoderRegistry<DecisionTreeCondition> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_condition"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("threshold"), ThresholdDecisionTreeCondition.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("script"   ),    ScriptDecisionTreeCondition.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("and"      ),       AndDecisionTreeCondition.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("or"       ),        OrDecisionTreeCondition.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("not"      ),       NotDecisionTreeCondition.class);
	}};

	public abstract ConditionTree createCondition(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		long selfSeed,
		DataCompileContext context,
		@Nullable InsnTree loadY
	)
	throws ScriptParsingException;

	public static abstract class Impl implements DecisionTreeCondition {

		public final Set<RegistryEntry<? extends ColumnValueDependencyHolder>> dependencies = new HashSet<>();

		@Override
		public Set<RegistryEntry<? extends ColumnValueDependencyHolder>> getDependencies() {
			return this.dependencies;
		}
	}
}