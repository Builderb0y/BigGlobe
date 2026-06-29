package builderb0y.bigglobe.versions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

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

	public static boolean dangerousRapids(ServerLevel world) {
		return world.getGameRules().get(BigGlobeGameRules.DANGEROUS_RAPIDS);
	}
}