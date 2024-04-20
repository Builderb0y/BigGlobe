package builderb0y.bigglobe.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.command.argument.serialize.ArgumentSerializer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.commands.EnumArgument.EnumArgumentSerializer;

public class BigGlobeArgumentTypes {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering command argument types...");
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("enum"),
			(Class)(EnumArgument.class),
			(ArgumentSerializer)(new EnumArgumentSerializer())
		);
		BigGlobeMod.LOGGER.debug("Done registering command argument types.");
	}
}