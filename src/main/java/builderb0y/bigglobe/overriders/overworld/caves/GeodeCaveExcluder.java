package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.structures.GeodeStructure;

public class GeodeCaveExcluder extends StructureCaveExcluder {

	public static final GeodeCaveExcluder INSTANCE = new GeodeCaveExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof GeodeStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		GeodeStructure.MainPiece.Data data = ((GeodeStructure.MainPiece)(start.getChildren().get(0))).data;
		CaveExclusionShapes.excludeSphere(
			context,
			data.x(),
			data.y(),
			data.z(),
			data.radius(),
			data.radius() + STRUCTURE_PADDING
		);
	}
}