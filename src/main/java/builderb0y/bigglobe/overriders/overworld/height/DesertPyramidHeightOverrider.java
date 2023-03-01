package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;
import builderb0y.bigglobe.structures.BiggerDesertPyramidStructure;

public class DesertPyramidHeightOverrider extends StructureHeightOverrider {

	public static final DesertPyramidHeightOverrider INSTANCE = new DesertPyramidHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof BiggerDesertPyramidStructure;
	}

	@Override
	public void override(Context context, StructureStart start) {
		context.fitToBottomOfBox(
			start.getBoundingBox().expand(-MAX_STRUCTURE_PADDING),
			16.5D,
			MAX_STRUCTURE_PADDING,
			SnowMixPolicy.SKIP_SNOW
		);
	}
}