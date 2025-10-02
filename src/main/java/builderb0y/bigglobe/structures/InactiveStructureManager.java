package builderb0y.bigglobe.structures;

public class InactiveStructureManager extends StructureManager {

	public static final FinalStructures EMPTY_STRUCTURES = new FinalStructures(0);

	@Override
	public FinalStructures getIntersectingStructures(StructureGenerationParams params) {
		return EMPTY_STRUCTURES;
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