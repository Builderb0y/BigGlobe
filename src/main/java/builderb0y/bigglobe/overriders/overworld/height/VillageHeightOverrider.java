package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.structure.JigsawStructure;

import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;

public class VillageHeightOverrider extends StructureHeightOverrider {

	public static final VillageHeightOverrider INSTANCE = new VillageHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof JigsawStructure && start.getStructure().getFeatureGenerationStep() == Feature.SURFACE_STRUCTURES;
	}

	@Override
	public void override(Context context, StructureStart start) {
		for (StructurePiece child : start.getChildren()) {
			if (
				child instanceof PoolStructurePiece pool &&
				pool.getPoolElement().getProjection() == Projection.RIGID
			) {
				context.fitToBottomOfBox(
					pool.getBoundingBox(),
					pool.getGroundLevelDelta() - 0.5D,
					8.0D,
					SnowMixPolicy.MIX_DOWN_ONLY
				);
			}
		}
	}
}