package builderb0y.bigglobe.columns.scripted.decisionTrees.conditions;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeException;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ThresholdDecisionTreeCondition extends AbstractThresholdDecisionTreeCondition {

	public final Holder<ColumnEntry> column_value;

	public ThresholdDecisionTreeCondition(
		Holder<ColumnEntry> column_value,
		double min,
		double max,
		@DefaultBoolean(true) boolean smooth_min,
		@DefaultBoolean(true) boolean smooth_max
	) {
		super(min, max, smooth_min, smooth_max);
		this.column_value = column_value;
	}

	@Override
	public ConditionTree createCondition(Holder<DecisionTreeSettings> selfEntry, long selfSeed, DataCompileContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		List<Holder<VoronoiSettings>> enablers = context.root().registry.voronoiManager.getEnablingSettings(this.column_value.value());
		if (!enablers.isEmpty()) {
			throw new DecisionTreeException("Column value " + UnregisteredObjectException.getID(this.column_value) + " is enabled by " + enablers.stream().map(UnregisteredObjectException::getID).map(Identifier::toString).collect(Collectors.joining(", ", "[ ", " ]")) + ", and therefore cannot be used as a decision tree threshold.");
		}
		ColumnEntryMemory memory = context.root().registry.columnContext.memories.get(this.column_value.value());
		if (memory == null) {
			throw new DecisionTreeException("Column value " + UnregisteredObjectException.getID(this.column_value) + " has no memory???");
		}
		this.addDependency(memory.getTyped(ColumnEntryMemory.REGISTRY_ENTRY));
		MethodCompileContext getter = memory.getTyped(ColumnEntryMemory.GETTER);
		boolean requiresY = getter.info.paramTypes.length != 0;
		if (requiresY && loadY == null) {
			throw new DecisionTreeException(UnregisteredObjectException.getID(this.column_value) + " is 3D, but a Y level is not provided.");
		}
		return this.finishCondition(
			invokeInstance(
				context.loadColumn(),
				getter.info,
				requiresY
					? new InsnTree[] { loadY }
					: InsnTree.ARRAY_FACTORY.empty()
			),
			selfSeed,
			context
		);
	}

	@Override
	public String errorMessage(TypeInfo type) {
		return "threshold type decision tree condition only works with float and double typed column values, but " + UnregisteredObjectException.getID(this.column_value) + " is a " + type.getSimpleName();
	}
}