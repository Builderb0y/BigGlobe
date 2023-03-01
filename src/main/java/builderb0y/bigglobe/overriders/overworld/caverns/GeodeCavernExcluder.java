package builderb0y.bigglobe.overriders.overworld.caverns;

import net.minecraft.structure.StructureStart;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.structures.GeodeStructure;

public class GeodeCavernExcluder extends StructureCavernExcluder {

	public static final GeodeCavernExcluder INSTANCE = new GeodeCavernExcluder();

	@Override
	public boolean shouldExclude(Context context, StructureStart start) {
		return start.getStructure() instanceof GeodeStructure;
	}

	@Override
	public void exclude(Context context, StructureStart start) {
		GeodeStructure.MainPiece.Data data = ((GeodeStructure.MainPiece)(start.getChildren().get(0))).data;
		double
			centerX   = data.x(),
			centerY   = data.y(),
			centerZ   = data.z(),
			radius    = data.radius(),
			relativeX = centerX - context.column.x,
			relativeZ = centerZ - context.column.z,
			distance  = Math.sqrt(BigGlobeMath.squareD(relativeX, relativeZ));
		context.exclude(
			context.getExclusionFactor(
				STRUCTURE_PADDING,
				centerY - radius,
				centerY + radius
			)
			* BigGlobeMath.squareD(
				Math.max(
					Interpolator.unmixLinear(
						radius + STRUCTURE_PADDING,
						radius,
						distance
					),
					0.0D
				)
			)
		);
	}
}