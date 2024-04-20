package builderb0y.bigglobe.commands;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

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

	public static final String NOT_APPLICABLE = "N/A";
	public static final DecimalFormat DECIMAL_FORMAT;
	static {
		DECIMAL_FORMAT = new DecimalFormat();
		DECIMAL_FORMAT.setDecimalSeparatorAlwaysShown(true);
		DECIMAL_FORMAT.setMinimumFractionDigits(1);
		DECIMAL_FORMAT.setMaximumFractionDigits(3);
		DecimalFormatSymbols symbols = DECIMAL_FORMAT.getDecimalFormatSymbols();
		symbols.setNaN(NOT_APPLICABLE);
		DECIMAL_FORMAT.setDecimalFormatSymbols(symbols);
	}

	public static String format(double number) {
		synchronized (DECIMAL_FORMAT) {
			return DECIMAL_FORMAT.format(number);
		}
	}

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
		LocateCommand        .register(dispatcher);
		RespawnCommand       .register(dispatcher);
		EvaluateCommand      .register(dispatcher);
		DumpRegistriesCommand.register(dispatcher);
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