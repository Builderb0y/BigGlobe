package builderb0y.bigglobe.gamerules;

import net.minecraft.server.MinecraftServer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;

#if MC_VERSION >= MC_1_21_11
	import net.minecraft.world.rule.GameRule;
	import net.minecraft.world.rule.GameRules;
	import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
	import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
#else
	import net.minecraft.world.GameRules;
	import net.minecraft.world.GameRules.BooleanRule;
	import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
	import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
	import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
#endif

public class BigGlobeGameRules {

	static { BigGlobeMod.LOGGER.debug("Registering game rules..."); }

	#if MC_VERSION >= MC_1_21_11

		public static final GameRule<Double> DAYLIGHT_CYCLE_SPEED = (
			GameRuleBuilder
			.forDouble(1.0D)
			.buildAndRegister(BigGlobeMod.modID("daylight_cycle_speed"))
		);
		static {
			GameRuleEvents.changeCallback(DAYLIGHT_CYCLE_SPEED).register((Double speed, MinecraftServer server) -> {
				server.getPlayerManager().getPlayerList().forEach(
					TimeSpeedS2CPacketHandler.INSTANCE::send
				);
			});
		}
		public static final GameRule<Boolean> SOUL_LAVA_SOURCE_CONVERSION = (
			GameRuleBuilder
			.forBoolean(false)
			.buildAndRegister(BigGlobeMod.modID("soul_lava_source_conversion"))
		);
		public static final GameRule<Boolean> DANGEROUS_RAPIDS = (
			GameRuleBuilder
			.forBoolean(true)
			.buildAndRegister(BigGlobeMod.modID("dangerous_rapids"))
		);
		static {
			GameRuleEvents.changeCallback(DANGEROUS_RAPIDS).register((Boolean rapids, MinecraftServer server) -> {
				server.getPlayerManager().getPlayerList().forEach(
					DangerousRapidsPacket.INSTANCE::send
				);
			});
		}

	#else

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
		public static final GameRules.Key<BooleanRule> SOUL_LAVA_SOURCE_CONVERSION = (
			GameRuleRegistry.register(
				"bigglobe:soulLavaSourceConversion",
				GameRules.Category.UPDATES,
				GameRuleFactory.createBooleanRule(false)
			)
		);
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

	#endif

	static { BigGlobeMod.LOGGER.debug("Done registering game rules."); }

	public static void init() {
		//trigger static initializer.
	}
}