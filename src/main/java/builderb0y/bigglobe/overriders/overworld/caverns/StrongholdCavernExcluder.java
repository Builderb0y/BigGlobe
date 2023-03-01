package builderb0y.bigglobe.overriders.overworld.caverns;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.structure.StrongholdStructure;

import builderb0y.bigglobe.math.Interpolator;

public class StrongholdCavernExcluder extends StructureCavernExcluder {

	public static final StrongholdCavernExcluder INSTANCE = new StrongholdCavernExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof StrongholdStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		BlockBox box = start.getBoundingBox().expand(-STRUCTURE_PADDING);
		int clampedX = MathHelper.clamp(context.column.x, box.getMinX(), box.getMaxX());
		int clampedZ = MathHelper.clamp(context.column.z, box.getMinZ(), box.getMaxZ());
		int distX = Math.abs(context.column.x - clampedX);
		int distZ = Math.abs(context.column.z - clampedZ);
		if (distX < STRUCTURE_PADDING && distZ < STRUCTURE_PADDING) {
			context.exclude(
				context.getExclusionFactor(
					STRUCTURE_PADDING,
					box.getMinY(),
					box.getMaxY() + 1
				)
				* Interpolator.smooth(1.0D - distX * (1.0D / STRUCTURE_PADDING))
				* Interpolator.smooth(1.0D - distZ * (1.0D / STRUCTURE_PADDING))
			);
		}
	}
}