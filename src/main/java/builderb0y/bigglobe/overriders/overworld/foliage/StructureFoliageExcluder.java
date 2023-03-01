package builderb0y.bigglobe.overriders.overworld.foliage;

import net.minecraft.structure.StructureStart;

public abstract class StructureFoliageExcluder implements OverworldFoliageExcluder {

	public abstract boolean shouldExclude(Context context, StructureStart start);

	public abstract void exclude(Context context, StructureStart start);

	@Override
	public void exclude(Context context) {
		for (StructureStart start : context.structures.starts) {
			if (this.shouldExclude(context, start)) {
				this.exclude(context, start);
			}
		}
	}
}