package builderb0y.bigglobe.versions;

import builderb0y.bigglobe.gamerules.BigGlobeGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

public class GameruleVersions {

	public static boolean entityDrops(ServerLevel world) {
		return world.getGameRules().get(GameRules.ENTITY_DROPS);
	}

	public static boolean tileDrops(ServerLevel world) {
		return world.getGameRules().get(GameRules.BLOCK_DROPS);
	}

	public static boolean soulLavaSourceConversion(ServerLevel world) {
		return world.getGameRules().get(BigGlobeGameRules.SOUL_LAVA_SOURCE_CONVERSION);
	}

	public static double daylightCycleSpeed(ServerLevel world) {
		return world.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED);
	}

	public static boolean dangerousRapids(ServerLevel world) {
		return world.getGameRules().get(BigGlobeGameRules.DANGEROUS_RAPIDS);
	}
}