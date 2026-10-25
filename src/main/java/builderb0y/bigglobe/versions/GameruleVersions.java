package builderb0y.bigglobe.versions;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

public class GameruleVersions {

	public static boolean entityDrops(Level world) {
		return world.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS);
	}

	public static boolean tileDrops(Level world) {
		return world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
	}

	public static boolean soulLavaSourceConversion(Level world) {
		return world.getGameRules().getBoolean(BigGlobeGameRules.SOUL_LAVA_SOURCE_CONVERSION);
	}

	public static double daylightCycleSpeed(Level world) {
		return world.getGameRules().getRule(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
	}

	public static boolean dangerousRapids(Level world) {
		return world.getGameRules().getBoolean(BigGlobeGameRules.DANGEROUS_RAPIDS);
	}
}
