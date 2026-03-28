package builderb0y.bigglobe.structures;

import java.util.Arrays;

import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;

public class InactiveStructureManager extends StructureManager {

	public static final FinalStructures EMPTY_STRUCTURES = new FinalStructures(0);

	@Override
	public FinalStructures getIntersectingStructures(StructureGenerationParams params) {
		return EMPTY_STRUCTURES;
	}

	@Override
	public ScriptStructures[] computeRelevantStructuresForOverriders(StructureGenerationParams params, ColumnValueOverridersWithRadiusCache overriders) {
		ScriptStructures[] result = new ScriptStructures[overriders.overriders().length];
		Arrays.fill(result, ScriptStructures.EMPTY_SCRIPT_STRUCTURES);
		return result;
	}

	@Override
	public FinalStructures getFinalStructures(StructureGenerationParams params) {
		return EMPTY_STRUCTURES;
	}

	@Override
	public StructureManager copy() {
		return new InactiveStructureManager();
	}
}