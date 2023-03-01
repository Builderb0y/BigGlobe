package builderb0y.bigglobe.overriders.overworld.height;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.overriders.overworld.height.OverworldHeightOverrider.Context.SnowMixPolicy;
import builderb0y.bigglobe.structures.LakeStructure;

public class LakeHeightOverrider extends StructureHeightOverrider {

	public static final LakeHeightOverrider INSTANCE = new LakeHeightOverrider();

	@Override
	public boolean shouldOverride(Context context, StructureStart start) {
		return start.getStructure() instanceof LakeStructure;
	}

	@Override
	public void override(Context context, StructureStart start) {
		LakeStructure.Piece piece = (LakeStructure.Piece)(start.getChildren().get(0));
		double distance = Math.sqrt(
			BigGlobeMath.squareD(
				context.column.x - piece.data.x(),
				context.column.z - piece.data.z()
			)
		);
		double radius = piece.data.horizontalRadius();
		if (distance < radius + MAX_STRUCTURE_PADDING) {
			context.mixHeightIncreaseOnly(
				piece.data.y(),
				Interpolator.unmixSmooth(
					radius + MAX_STRUCTURE_PADDING,
					radius,
					distance
				),
				SnowMixPolicy.ADD
			);
		}
		if (distance < radius) {
			context.add(piece.getDip(context.column.x, context.column.z, distance));
		}
	}
}