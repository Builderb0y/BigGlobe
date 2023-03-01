package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;
import builderb0y.bigglobe.structures.CampfireStructure;

public class CampfireHeightOverrider extends StructureHeightOverrider {

	public static final CampfireHeightOverrider INSTANCE = new CampfireHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof CampfireStructure;
	}

	@Override
	public void override(Context context, StructureStart start) {
		for (StructurePiece child : start.getChildren()) {
			context.fitToBottomOfBox(child.getBoundingBox(), 0.5D, 8.0D, SnowMixPolicy.MIX_DOWN_ONLY);
		}
	}
}