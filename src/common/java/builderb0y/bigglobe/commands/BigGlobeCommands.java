package builderb0y.bigglobe.commands;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

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
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		CommandSelection environment
	) {
		BigGlobeMod.LOGGER.debug("Registering commands to dispatcher...");
		BigGlobeLocateCommand.register(dispatcher);
		RespawnCommand.register(dispatcher);
		EvaluateCommand.register(dispatcher);
		DumpRegistriesCommand.register(dispatcher);
		DevDebugCommand.register(dispatcher, registryAccess);
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
		CommandBuildContext registryAccess
	) {
		BigGlobeMod.LOGGER.debug("Registering client commands to dispatcher...");
		DisplayColumnsClientCommand.register(dispatcher);
		SearchF3ClientCommand.register(dispatcher);
		BigGlobeMod.LOGGER.debug("Done registering client commands to dispatcher.");
	}

	public static BigGlobeScriptedChunkGenerator generator(CommandContext<CommandSourceStack> context) {
		return generator(context.getSource());
	}

	public static BigGlobeScriptedChunkGenerator generator(CommandSourceStack source) {
		return source.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}
}