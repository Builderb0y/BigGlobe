package builderb0y.bigglobe.overriders.overworld.caverns;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.structures.dungeons.AbstractDungeonStructure;

public class DungeonCavernExcluder extends StructureCavernExcluder {

	public static final DungeonCavernExcluder INSTANCE = new DungeonCavernExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof AbstractDungeonStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		BlockBox box = start.getBoundingBox().expand(-STRUCTURE_PADDING);
		double
			centerX = (box.getMaxX() + 1 + box.getMinX()) * 0.5D,
			centerZ = (box.getMaxZ() + 1 + box.getMinZ()) * 0.5D,
			radius  = (
				Math.max(
					box.getMaxX() + 1 - box.getMinX(),
					box.getMaxZ() + 1 - box.getMinZ()
				)
				* 0.5D
			),
			relativeX = centerX - context.column.x,
			relativeZ = centerZ - context.column.z,
			distance  = Math.sqrt(BigGlobeMath.squareD(relativeX, relativeZ));
		context.exclude(
			context.getExclusionFactor(
				STRUCTURE_PADDING,
				box.getMinY(),
				box.getMaxY() + 1
			)
			* BigGlobeMath.squareD(
				Math.max(
					Interpolator.unmixLinear(
						radius + STRUCTURE_PADDING,
						radius,
						distance
					),
					0.0D
				)
			)
		);
	}
}