package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.structure.IglooStructure;

import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;

public class IglooHeightOverrider extends StructureHeightOverrider {

	public static final IglooHeightOverrider INSTANCE = new IglooHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof IglooStructure;
	}

	@Override
	public void override(Context context, StructureStart start) {
		context.fitToBottomOfBox(
			start.getChildren().get(start.getChildren().size() - 1).getBoundingBox(),
			1.0625D,
			MAX_STRUCTURE_PADDING,
			SnowMixPolicy.SNOW_ONLY
		);
	}
}