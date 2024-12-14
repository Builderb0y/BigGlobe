package builderb0y.bigglobe.versions;

import net.minecraft.util.math.Direction;

public class DirectionVersions {

	public static int horizontal(Direction direction) {
		#if MC_VERSION >= MC_1_21_4
			return direction.getHorizontalQuarterTurns();
		#else
			return direction.getHorizontal();
		#endif
	}

	public static float horizontalAngle(Direction direction) {
		#if MC_VERSION >= MC_1_21_4
			return direction.getPositiveHorizontalDegrees();
		#else
			return direction.asRotation();
		#endif
	}
}