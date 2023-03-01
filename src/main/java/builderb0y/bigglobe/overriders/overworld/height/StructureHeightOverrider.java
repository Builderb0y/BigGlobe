package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructureStart;

public abstract class StructureHeightOverrider implements OverworldHeightOverrider {

	public static final int MAX_STRUCTURE_PADDING = 16;

	public abstract boolean shouldOverride(Context context, StructureStart start);

	public abstract void override(Context context, StructureStart start);

	@Override
	public void override(Context context) {
		for (StructureStart start : context.structures.starts) {
			if (this.shouldOverride(context, start)) {
				this.override(context, start);
			}
		}
	}
}