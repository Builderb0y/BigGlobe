package builderb0y.bigglobe.commands;

import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixinInterfaces.SearchableDebugHud;
import builderb0y.bigglobe.mixins.InGameHud_DebugHudGetter;

public class SearchF3ClientCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommands
			.literal(BigGlobeMod.MODID + ":searchF3")
			.executes(context -> {
				(
					(SearchableDebugHud)(
						(
							(InGameHud_DebugHudGetter)(
								Minecraft.getInstance().gui
							)
						)
						.bigglobe_getDebugHud()
					)
				)
				.bigglobe_setPattern(null);
				return 1;
			})
			.then(
				ClientCommands
				.argument("pattern", StringArgumentType.greedyString())
				.executes(context -> {
					Pattern pattern = Pattern.compile(context.getArgument("pattern", String.class));
					(
						(SearchableDebugHud)(
							(
								(InGameHud_DebugHudGetter)(
									Minecraft.getInstance().gui
								)
							)
							.bigglobe_getDebugHud()
						)
					)
					.bigglobe_setPattern(pattern);
					return 1;
				})
			)
		);
	}
}