package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.structures.LakeStructure;

public class LakeFoliageExcluder extends StructureFoliageExcluder {

	public static final LakeFoliageExcluder INSTANCE = new LakeFoliageExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof LakeStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		context.exclude(context.column.getInLake());
	}
}