package builderb0y.bigglobe.overriders.overworld.caverns;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.structure.StructureKeys;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public class AncientCityCavernExcluder extends StructureCavernExcluder {

	public static final AncientCityCavernExcluder INSTANCE = new AncientCityCavernExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return BigGlobeMod.getCurrentServer().getRegistryManager().get(Registry.STRUCTURE_KEY).getKey(start.getStructure()).orElse(null) == StructureKeys.ANCIENT_CITY;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		double factor = 1.0D;
		for (StructurePiece piece : start.getChildren()) {
			BlockBox box = piece.getBoundingBox();
			int clampedRelativeX = MathHelper.clamp(context.column.x, box.getMinX(), box.getMaxX()) - context.column.x;
			int clampedRelativeZ = MathHelper.clamp(context.column.z, box.getMinZ(), box.getMaxZ()) - context.column.z;
			factor *= Math.min(((double)(BigGlobeMath.squareI(clampedRelativeX, clampedRelativeZ))) / ((double)(STRUCTURE_PADDING * STRUCTURE_PADDING)), 1.0D);
		}
		if (factor >= 1.0D) return;
		BlockBox box = start.getBoundingBox().expand(-STRUCTURE_PADDING);
		double centerY = (box.getMinY() + box.getMaxY()) * 0.5D + 2.0D;
		double thickness = (box.getMaxY() - box.getMinY()) * 0.5D + 0.75D;
		//double factor = Interpolator.smooth(Math.sqrt(horizontalDistanceSquared) / STRUCTURE_PADDING);
		context.column.cavernCenter = Interpolator.mixLinear(centerY, context.column.cavernCenter, factor);
		context.column.cavernThicknessSquared = Interpolator.mixLinear(BigGlobeMath.squareD(thickness), (context.column.cavernThicknessSquared > 0.0D ? context.column.cavernThicknessSquared : 0.0D), factor);
	}
}