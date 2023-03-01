package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.structures.dungeons.AbstractDungeonStructure;

public class DungeonCaveExcluder extends StructureCaveExcluder {

	public static final DungeonCaveExcluder INSTANCE = new DungeonCaveExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof AbstractDungeonStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		BlockBox box = start.getBoundingBox().expand(-STRUCTURE_PADDING);
		double centerX = (box.getMaxX() + 1 + box.getMinX()) * 0.5D;
		double centerZ = (box.getMaxZ() + 1 + box.getMinZ()) * 0.5D;
		double radius = (
			Math.max(
				box.getMaxX() + 1 - box.getMinX(),
				box.getMaxZ() + 1 - box.getMinZ()
			)
			* 0.5D
		);
		CaveExclusionShapes.excludeCylinder(
			context,
			centerX,
			centerZ,
			box.getMinY(),
			box.getMaxY(),
			radius,
			STRUCTURE_PADDING
		);
	}
}