package builderb0y.bigglobe.versions;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.gamerules.BigGlobeGameRules;

#if MC_VERSION >= MC_1_21_11
	import net.minecraft.world.rule.GameRules;
#else
	import net.minecraft.world.GameRules;
#endif

public class GameruleVersions {

	#if MC_VERSION >= MC_1_21_11

		public static boolean entityDrops(ServerWorld world) {
			return world.getGameRules().getValue(GameRules.ENTITY_DROPS);
		}

		public static boolean tileDrops(ServerWorld world) {
			return world.getGameRules().getValue(GameRules.DO_TILE_DROPS);
		}

		public static boolean soulLavaSourceConversion(ServerWorld world) {
			return world.getGameRules().getValue(BigGlobeGameRules.SOUL_LAVA_SOURCE_CONVERSION);
		}

		public static double daylightCycleSpeed(ServerWorld world) {
			return world.getGameRules().getValue(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED);
		}

		public static boolean dangerousRapids(ServerWorld world) {
			return world.getGameRules().getValue(BigGlobeGameRules.DANGEROUS_RAPIDS);
		}

	#elif MC_VERSION >= MC_1_21_3

		public static boolean entityDrops(ServerWorld world) {
			return world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS);
		}

		public static boolean tileDrops(ServerWorld world) {
			return world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS);
		}

		public static boolean soulLavaSourceConversion(ServerWorld world) {
			return world.getGameRules().getBoolean(BigGlobeGameRules.SOUL_LAVA_SOURCE_CONVERSION);
		}

		public static double daylightCycleSpeed(ServerWorld world) {
			return world.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		}

		public static boolean dangerousRapids(ServerWorld world) {
			return world.getGameRules().getBoolean(BigGlobeGameRules.DANGEROUS_RAPIDS);
		}

	#else

		public static boolean entityDrops(World world) {
			return world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS);
		}

		public static boolean tileDrops(World world) {
			return world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS);
		}

		public static boolean soulLavaSourceConversion(World world) {
			return world.getGameRules().getBoolean(BigGlobeGameRules.SOUL_LAVA_SOURCE_CONVERSION);
		}

		public static double daylightCycleSpeed(World world) {
			return world.getGameRules().get(BigGlobeGameRules.DAYLIGHT_CYCLE_SPEED).get();
		}

		public static boolean dangerousRapids(World world) {
			return world.getGameRules().getBoolean(BigGlobeGameRules.DANGEROUS_RAPIDS);
		}

	#endif
}