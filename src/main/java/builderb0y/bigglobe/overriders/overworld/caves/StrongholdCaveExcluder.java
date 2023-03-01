package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.gen.structure.StrongholdStructure;

public class StrongholdCaveExcluder extends StructureCaveExcluder {

	public static final StrongholdCaveExcluder INSTANCE = new StrongholdCaveExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof StrongholdStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		BlockBox box = start.getBoundingBox().expand(-STRUCTURE_PADDING);
		CaveExclusionShapes.excludeCuboid(context, box, STRUCTURE_PADDING);
	}
}