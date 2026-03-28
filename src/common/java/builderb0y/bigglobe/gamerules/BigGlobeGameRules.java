package builderb0y.bigglobe.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;

public class BigGlobeGameRules {

	static {
		BigGlobeMod.LOGGER.debug("Registering game rules...");
	}

	public static final GameRule<Double> DAYLIGHT_CYCLE_SPEED = (
		GameRuleBuilder
			.forDouble(1.0D)
			.buildAndRegister(BigGlobeMod.modID("daylight_cycle_speed"))
	);

	static {
		GameRuleEvents.changeCallback(DAYLIGHT_CYCLE_SPEED).register((Double speed, MinecraftServer server) -> {
			server.getPlayerList().getPlayers().forEach(
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
			server.getPlayerList().getPlayers().forEach(
				DangerousRapidsPacket.INSTANCE::send
			);
		});
	}

	static {
		BigGlobeMod.LOGGER.debug("Done registering game rules.");
	}

	public static void init() {
		//trigger static initializer.
	}
}