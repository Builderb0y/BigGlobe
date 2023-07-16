package builderb0y.bigglobe.commands;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;

import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.commands.EnumArgument.EnumArgumentSerializer;

public class BigGlobeArgumentTypes {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering command argument types...");
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("column_value"),
			ColumnValueArgument.class,
			ConstantArgumentSerializer.of(ColumnValueArgument::new)
		);
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("enum"),
			(Class)(EnumArgument.class),
			(ArgumentSerializer)(new EnumArgumentSerializer())
		);
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("world_script"),
			CommandScriptArgument.class,
			ConstantArgumentSerializer.of(CommandScriptArgument::new)
		);
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("locate_noise_script"),
			LocateNoiseLazyScript.Argument.class,
			ConstantArgumentSerializer.of(LocateNoiseLazyScript.Argument::new)
		);
		ArgumentTypeRegistry.registerArgumentType(
			BigGlobeMod.modID("locate_area_script"),
			LocateAreaLazyScript.Argument.class,
			ConstantArgumentSerializer.of(LocateAreaLazyScript.Argument::new)
		);
		BigGlobeMod.LOGGER.debug("Done registering command argument types.");
	}
}