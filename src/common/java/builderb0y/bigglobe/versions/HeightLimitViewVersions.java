package builderb0y.bigglobe.versions;

import net.minecraft.world.level.LevelHeightAccessor;

public class HeightLimitViewVersions {

	public static int getMinY(LevelHeightAccessor view) {
		return view.getMinY();
	}

	public static int getMaxY(LevelHeightAccessor view) {
		return view.getMinY() + view.getHeight();
	}

	public static int getHeight(LevelHeightAccessor view) {
		return view.getHeight();
	}

	public static int getSectionMinY(LevelHeightAccessor view) {
		return getMinY(view) >> 4;
	}

	public static int getSectionMaxY(LevelHeightAccessor view) {
		return getMaxY(view) >> 4;
	}

	public static int getSectionHeight(LevelHeightAccessor view) {
		return getHeight(view) >> 4;
	}
}