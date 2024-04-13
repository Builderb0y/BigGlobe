package builderb0y.bigglobe.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;

public class BigGlobeGameRules {

	static { BigGlobeMod.LOGGER.debug("Registering game rules..."); }

	public static final GameRules.Key<DoubleRule> DAYLIGHT_CYCLE_SPEED = (
		GameRuleRegistry.register(
			"bigglobe:daylightCycleSpeed",
			GameRules.Category.UPDATES,
			GameRuleFactory.createDoubleRule(1.0D, 0.0D, (MinecraftServer server, DoubleRule rule) -> {
				server.getPlayerManager().getPlayerList().forEach(
					TimeSpeedS2CPacketHandler.INSTANCE::send
				);
			})
		)
	);
	#if MC_VERSION > MC_1_19_2
		public static final GameRules.Key<BooleanRule> SOUL_LAVA_SOURCE_CONVERSION = (
			GameRuleRegistry.register(
				"bigglobe:soulLavaSourceConversion",
				GameRules.Category.UPDATES,
				GameRuleFactory.createBooleanRule(false)
			)
		);
	#endif
	public static final GameRules.Key<BooleanRule> DANGEROUS_RAPIDS = (
		GameRuleRegistry.register(
			"bigglobe:dangerousRapids",
			GameRules.Category.UPDATES,
			GameRuleFactory.createBooleanRule(true, (MinecraftServer server, BooleanRule rule) -> {
				server.getPlayerManager().getPlayerList().forEach(
					DangerousRapidsPacket.INSTANCE::send
				);
			})
		)
	);

	static { BigGlobeMod.LOGGER.debug("Done registering game rules."); }

	public static void init() {
		//trigger static initializer.
	}
}