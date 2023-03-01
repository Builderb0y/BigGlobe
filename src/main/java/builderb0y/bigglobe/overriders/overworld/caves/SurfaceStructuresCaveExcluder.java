package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.structure.DesertPyramidStructure;
import net.minecraft.world.gen.structure.JigsawStructure;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.structures.BiggerDesertPyramidStructure;
import builderb0y.bigglobe.structures.CampfireStructure;

public class SurfaceStructuresCaveExcluder extends StructureCaveExcluder {

	public static final SurfaceStructuresCaveExcluder INSTANCE = new SurfaceStructuresCaveExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return (
			(
				start.getStructure() instanceof JigsawStructure &&
				start.getStructure().getFeatureGenerationStep() == Feature.SURFACE_STRUCTURES
			)
			||
			start.getStructure() instanceof            CampfireStructure ||
			start.getStructure() instanceof       DesertPyramidStructure ||
			start.getStructure() instanceof BiggerDesertPyramidStructure
		);
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		for (StructurePiece child : start.getChildren()) {
			BlockBox box = child.getBoundingBox();
			int clampX = MathHelper.clamp(context.column.x, box.getMinX(), box.getMaxX());
			int clampZ = MathHelper.clamp(context.column.z, box.getMinZ(), box.getMaxZ());
			double offsetX = 1.0D - Math.abs(context.column.x - clampX) * 0.125D;
			double offsetZ = 1.0D - Math.abs(context.column.z - clampZ) * 0.125D;
			if (offsetX <= 0.0D || offsetZ <= 0.0D) continue;
			double multiplier = Interpolator.smooth(offsetX) * Interpolator.smooth(offsetZ);
			context.excludeSurface(multiplier);
		}
	}
}