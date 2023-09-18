package builderb0y.bigglobe.commands;

import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.MinecraftClient;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixinInterfaces.SearchableDebugHud;
import builderb0y.bigglobe.mixins.InGameHud_DebugHudGetter;

public class SearchF3ClientCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommandManager
			.literal(BigGlobeMod.MODID + ":searchF3")
			.executes(context -> {
				(
					(SearchableDebugHud)(
						(
							(InGameHud_DebugHudGetter)(
								MinecraftClient.getInstance().inGameHud
							)
						)
						.bigglobe_getDebugHud()
					)
				)
				.bigglobe_setPattern(null);
				return 1;
			})
			.then(
				ClientCommandManager
				.argument("pattern", StringArgumentType.greedyString())
				.executes(context -> {
					Pattern pattern = Pattern.compile(context.getArgument("pattern", String.class));
					(
						(SearchableDebugHud)(
							(
								(InGameHud_DebugHudGetter)(
									MinecraftClient.getInstance().inGameHud
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