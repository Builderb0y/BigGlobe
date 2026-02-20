package builderb0y.bigglobe.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.commands.RespawnCommand.RespawnModeArgumentType;

public class BigGlobeArgumentTypes {

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering command argument types...");
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("respawn_mode"),
			RespawnModeArgumentType.class,
			ConstantArgumentSerializer.of(RespawnModeArgumentType::new)
		);
		BigGlobeMod.LOGGER.debug("Done registering command argument types.");
	}
}