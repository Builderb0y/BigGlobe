package builderb0y.bigglobe.overriders.overworld.caves;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public class CaveExclusionShapes {

	public static void excludeCuboid(
		OverworldCaveExcluder.Context context,
		BlockBox box,
		int radius
	) {
		int clampedRelativeX = MathHelper.clamp(context.column.x, box.getMinX(), box.getMaxX()) - context.column.x;
		int clampedRelativeZ = MathHelper.clamp(context.column.z, box.getMinZ(), box.getMaxZ()) - context.column.z;
		int horizontalDistanceSquared = BigGlobeMath.squareI(clampedRelativeX, clampedRelativeZ);
		if (horizontalDistanceSquared >= radius * radius) return;
		int minY = Math.max(box.getMinY() - radius, context.bottomI);
		int maxY = Math.min(box.getMaxY() + radius, context.topI - 1);
		for (int y = minY; y <= maxY; y++) {
			int clampedRelativeY = MathHelper.clamp(y, box.getMinY(), box.getMaxY()) - y;
			int distanceSquared = horizontalDistanceSquared + clampedRelativeY * clampedRelativeY;
			if (distanceSquared < radius * radius) {
				double fraction = 1.0D - Math.sqrt(distanceSquared) / radius;
				context.excludeUnchecked(y, fraction * fraction);
			}
		}
	}

	public static void excludeCylinder(
		OverworldCaveExcluder.Context context,
		double centerX,
		double centerZ,
		double bottomY,
		double topY,
		double radius,
		double padding
	) {
		double radiusSquared = radius * radius;
		double clampedRelativeX = context.column.x - centerX;
		double clampedRelativeZ = context.column.z - centerZ;
		double horizontalDistanceSquared = BigGlobeMath.squareD(clampedRelativeX, clampedRelativeZ);
		if (horizontalDistanceSquared > radiusSquared) {
			horizontalDistanceSquared = BigGlobeMath.squareD(Math.sqrt(horizontalDistanceSquared) - radius);
			if (horizontalDistanceSquared >= padding * padding) return;
		}
		else {
			horizontalDistanceSquared = 0.0D;
		}
		int minY = Math.max(BigGlobeMath.ceilI(bottomY - padding), context.bottomI);
		int maxY = Math.min(BigGlobeMath.floorI(topY + padding), context.topI - 1);
		for (int y = minY; y <= maxY; y++) {
			double verticalDistance = MathHelper.clamp(y, bottomY, topY) - y;
			double distanceSquared = horizontalDistanceSquared + verticalDistance * verticalDistance;
			if (distanceSquared < padding * padding) {
				double fraction = 1.0D - Math.sqrt(distanceSquared) / padding;
				context.excludeUnchecked(y, fraction * fraction);
			}
		}
	}

	public static void excludeSphere(
		OverworldCaveExcluder.Context context,
		double centerX,
		double centerY,
		double centerZ,
		double innerRadius,
		double outerRadius
	) {
		double
			innerRadiusSquared = innerRadius * innerRadius,
			outerRadiusSquared = outerRadius * outerRadius,
			offsetX            = context.column.x - centerX,
			offsetZ            = context.column.z - centerZ,
			distanceXZSquared  = BigGlobeMath.squareD(offsetX, offsetZ);
		if (distanceXZSquared >= outerRadius * outerRadius) return;
		int
			minY = Math.max(BigGlobeMath. ceilI(centerY - outerRadius), context.bottomI),
			maxY = Math.min(BigGlobeMath.floorI(centerY + outerRadius), context.topI - 1),
			startY = MathHelper.clamp(BigGlobeMath.floorI(centerY), minY, maxY);

		for (int y = startY; ++y <= maxY;) {
			double offsetY = y - centerY;
			double distanceXYZSquared = distanceXZSquared + offsetY * offsetY;
			if (distanceXYZSquared <= innerRadiusSquared) {
				context.excludeUnchecked(y, 1.0D);
			}
			else if (distanceXYZSquared <= outerRadiusSquared) {
				double distanceXYZ = Math.sqrt(distanceXYZSquared);
				double exclusion = Interpolator.unmixLinear(outerRadius, innerRadius, distanceXYZ);
				//float exclusion = (float)((outerRadius - distanceXYZ) / (outerRadius - innerRadius));
				context.excludeUnchecked(y, exclusion * exclusion);
			}
			else {
				break;
			}
		}
		for (int y = startY; y >= minY; y--) {
			double offsetY = y - centerY;
			double distanceXYZSquared = distanceXZSquared + offsetY * offsetY;
			if (distanceXYZSquared <= innerRadiusSquared) {
				context.excludeUnchecked(y, 1.0D);
			}
			else if (distanceXYZSquared <= outerRadiusSquared) {
				double distanceXYZ = Math.sqrt(distanceXYZSquared);
				float exclusion = (float)(Interpolator.unmixLinear(outerRadius, innerRadius, distanceXYZ));
				//float exclusion = (outerRadius - distanceXYZ) / (outerRadius - innerRadius);
				context.excludeUnchecked(y, exclusion * exclusion);
			}
			else {
				break;
			}
		}
	}
}