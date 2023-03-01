package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.overriders.overworld.OverworldOverrideContext.OverridePhase;
import builderb0y.bigglobe.structures.CampfireStructure;

public class CampfireFoliageExcluder extends StructureFoliageExcluder {

	public static final CampfireFoliageExcluder INSTANCE = new CampfireFoliageExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof CampfireStructure && context.phase == OverridePhase.DECORATION;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		for (StructurePiece child : start.getChildren()) {
			context.excludeBox(child.getBoundingBox(), 8);
		}
	}
}