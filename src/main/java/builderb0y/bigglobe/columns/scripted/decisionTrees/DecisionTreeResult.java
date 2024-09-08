package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

@UseCoder(name = "REGISTRY", in = DecisionTreeResult.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface DecisionTreeResult extends CoderRegistryTyped<DecisionTreeResult>, SimpleDependencyView {

	public static final CoderRegistry<DecisionTreeResult> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_result"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"), ConstantDecisionTreeResult.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("scripted"), ScriptedDecitionTreeResult.class);
	}};

	public abstract InsnTree createResult(RegistryEntry<DecisionTreeSettings> selfEntry, DataCompileContext context, AccessSchema accessSchema, @Nullable InsnTree loadY) throws ScriptParsingException;
}