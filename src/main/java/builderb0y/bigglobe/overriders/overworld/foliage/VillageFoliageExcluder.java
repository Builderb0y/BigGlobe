package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.structure.JigsawStructure;

public class VillageFoliageExcluder extends StructureFoliageExcluder {

	public static final VillageFoliageExcluder INSTANCE = new VillageFoliageExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof JigsawStructure && start.getStructure().getFeatureGenerationStep() == Feature.SURFACE_STRUCTURES;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		for (StructurePiece child : start.getChildren()) {
			context.excludeBox(child.getBoundingBox(), 8);
		}
	}
}