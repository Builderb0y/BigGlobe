package builderb0y.bigglobe.columns.scripted.decisionTrees.conditions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeException;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.traits.TraitManager.TraitInfo;
import builderb0y.bigglobe.columns.scripted.traits.WorldTrait;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits2DGetterInsnTree;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneTraits3DGetterInsnTree;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldTraitDecisionTreeCondition extends AbstractThresholdDecisionTreeCondition {

	public final RegistryEntry<WorldTrait> trait;

	public WorldTraitDecisionTreeCondition(
		RegistryEntry<WorldTrait> trait,
		double min,
		double max,
		@DefaultBoolean(true) boolean smooth_min,
		@DefaultBoolean(true) boolean smooth_max
	) {
		super(min, max, smooth_min, smooth_max);
		this.trait = trait;
		this.addDependency(trait);
	}

	@Override
	public ConditionTree createCondition(
		RegistryEntry<DecisionTreeSettings> selfEntry,
		long selfSeed,
		DataCompileContext context,
		@Nullable InsnTree loadY
	)
	throws ScriptParsingException {
		TraitInfo info = context.root().registry.traitManager.infos.get(this.trait);
		if (info == null) throw new IllegalStateException(UnregisteredObjectException.getKey(this.trait) + " didn't get compiled properly?");
		boolean requiresY = this.trait.value().schema().is_3d();
		if (requiresY && loadY == null) {
			throw new DecisionTreeException(UnregisteredObjectException.getKey(this.trait) + " is 3D, but a Y level is not provided.");
		}
		return this.finishCondition(
			requiresY
			? new StandAloneTraits3DGetterInsnTree(context.loadColumn(), loadY, info.getter.info, null)
			: new StandAloneTraits2DGetterInsnTree(context.loadColumn(),        info.getter.info, null),
			selfSeed,
			context
		);
	}

	@Override
	public String errorMessage(TypeInfo type) {
		return "world_trait_threshold decision tree condition only works with float and double typed world traits, but " + UnregisteredObjectException.getID(this.trait) + " is a " + type;
	}
}