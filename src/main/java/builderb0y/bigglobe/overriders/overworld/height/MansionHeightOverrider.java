package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructureStart;
import net.minecraft.world.gen.structure.WoodlandMansionStructure;

import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;

public class MansionHeightOverrider extends StructureHeightOverrider {

	public static final MansionHeightOverrider INSTANCE = new MansionHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof WoodlandMansionStructure;
	}

	@Override
	public void override(Context context, StructureStart start) {
		context.fitToBottomOfBox(
			start.getBoundingBox().expand(-MAX_STRUCTURE_PADDING),
			-0.5D,
			MAX_STRUCTURE_PADDING,
			SnowMixPolicy.MIX_DOWN_ONLY
		);
	}
}