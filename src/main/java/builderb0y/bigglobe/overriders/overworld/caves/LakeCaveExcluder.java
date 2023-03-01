package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.structures.LakeStructure;

public class LakeCaveExcluder extends StructureCaveExcluder {

	public static final LakeCaveExcluder INSTANCE = new LakeCaveExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof LakeStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		LakeStructure.Piece piece = (LakeStructure.Piece)(start.getChildren().get(0));
		CaveExclusionShapes.excludeCylinder(
			context,
			piece.data.x(),
			piece.data.z(),
			piece.getBoundingBox().getMinY(),
			piece.getBoundingBox().getMaxY(),
			piece.data.horizontalRadius(),
			8.0D
		);
	}
}