package builderb0y.bigglobe.versions;

import net.minecraft.core.Direction;

public class DirectionVersions {

	public static int horizontal(Direction direction) {

		return direction.get2DDataValue();
	}

	public static float horizontalAngle(Direction direction) {

		return direction.toYRot();
	}
}