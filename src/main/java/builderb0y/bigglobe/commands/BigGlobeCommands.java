package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeCommands {

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering command event handler...");
		CommandRegistrationCallback.EVENT.register(BigGlobeCommands::registerCommands);
		BigGlobeMod.LOGGER.debug("Done registering command event handler.");
	}

	public static void registerCommands(
		CommandDispatcher<ServerCommandSource> dispatcher,
		CommandRegistryAccess registryAccess,
		RegistrationEnvironment environment
	) {
		BigGlobeMod.LOGGER.debug("Registering commands to dispatcher...");
		LocateNoiseCommand    .register(dispatcher);
		LocateAreaCommand     .register(dispatcher);
		RespawnCommand        .register(dispatcher);
		EvaluateCommand       .register(dispatcher);
		WorldgenTimingsCommand.register(dispatcher);
		DumpRegistriesCommand .register(dispatcher);
		BigGlobeMod.LOGGER.debug("Done registering commands to dispatcher.");
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		BigGlobeMod.LOGGER.debug("Registering client command event handler...");
		ClientCommandRegistrationCallback.EVENT.register(BigGlobeCommands::registerClientCommands);
		BigGlobeMod.LOGGER.debug("Done registering client command event handler.");
	}

	@Environment(EnvType.CLIENT)
	public static void registerClientCommands(
		CommandDispatcher<FabricClientCommandSource> dispatcher,
		CommandRegistryAccess registryAccess
	) {
		BigGlobeMod.LOGGER.debug("Registering client commands to dispatcher...");
		DisplayColumnsClientCommand.register(dispatcher);
		SearchF3ClientCommand.register(dispatcher);
		BigGlobeMod.LOGGER.debug("Done registering client commands to dispatcher.");
	}
}