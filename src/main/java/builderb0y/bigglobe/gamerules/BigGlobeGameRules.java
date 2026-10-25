package builderb0y.bigglobe.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.networking.packets.DangerousRapidsPacket;
import builderb0y.bigglobe.networking.packets.TimeSpeedS2CPacketHandler;

public class BigGlobeGameRules {

	static {
		BigGlobeMod.LOGGER.debug("Registering game rules...");
	}

	public static final GameRules.Key<GameRuleBuilder.DoubleValue> DAYLIGHT_CYCLE_SPEED = (
		GameRuleBuilder
			.forDouble(1.0D)
			.buildAndRegister(BigGlobeMod.modID("daylight_cycle_speed"))
	);

	static {
		GameRuleEvents.<Double>changeCallback(DAYLIGHT_CYCLE_SPEED).register((Double speed, MinecraftServer server) -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				TimeSpeedS2CPacketHandler.INSTANCE.send(player);
			}
		});
	}

	public static final GameRules.Key<GameRules.BooleanValue> SOUL_LAVA_SOURCE_CONVERSION = (
		GameRuleBuilder
			.forBoolean(false)
			.buildAndRegister(BigGlobeMod.modID("soul_lava_source_conversion"))
	);
	public static final GameRules.Key<GameRules.BooleanValue> DANGEROUS_RAPIDS = (
		GameRuleBuilder
			.forBoolean(true)
			.buildAndRegister(BigGlobeMod.modID("dangerous_rapids"))
	);

	static {
		GameRuleEvents.<Boolean>changeCallback(DANGEROUS_RAPIDS).register((Boolean rapids, MinecraftServer server) -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				DangerousRapidsPacket.INSTANCE.send(player);
			}
		});
	}

	static {
		BigGlobeMod.LOGGER.debug("Done registering game rules.");
	}

	public static void init() {
		//trigger static initializer.
	}
}
