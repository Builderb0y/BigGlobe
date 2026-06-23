package builderb0y.bigglobe.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.commands.synchronization.SingletonArgumentInfo;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.commands.DevDebugCommand.MobCategoryArgument;
import builderb0y.bigglobe.commands.RespawnCommand.RespawnModeArgumentType;

public class BigGlobeArgumentTypes {

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering command argument types...");
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("respawn_mode"),
			RespawnModeArgumentType.class,
			SingletonArgumentInfo.contextFree(RespawnModeArgumentType::new)
		);
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("mob_category"),
			MobCategoryArgument.class,
			SingletonArgumentInfo.contextFree(MobCategoryArgument::new)
		);
		BigGlobeMod.LOGGER.debug("Done registering command argument types.");
	}
}