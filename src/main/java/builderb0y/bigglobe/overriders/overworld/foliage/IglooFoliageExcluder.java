package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.structure.IglooStructure;

public class IglooFoliageExcluder extends StructureFoliageExcluder {

	public static final IglooFoliageExcluder INSTANCE = new IglooFoliageExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof IglooStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		context.excludeBox(
			start.getChildren().get(start.getChildren().size() - 1).getBoundingBox(),
			8
		);
	}
}