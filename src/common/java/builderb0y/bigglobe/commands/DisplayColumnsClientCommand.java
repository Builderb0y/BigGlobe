package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Environment(EnvType.CLIENT)
public class DisplayColumnsClientCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommands
			.literal(BigGlobeMod.MODID + ":displayColumns")
			.requires((FabricClientCommandSource source) -> getGenerator(source) != null)
			.executes((CommandContext<FabricClientCommandSource> context) -> {
				BigGlobeScriptedChunkGenerator generator = getGenerator(context.getSource());
				if (generator != null) {
					generator.setDisplay(null);
					return 1;
				}
				else {
					return 0;
				}
			})
			.then(
				ClientCommands
				.argument("filter", StringArgumentType.greedyString())
				.executes((CommandContext<FabricClientCommandSource> context) -> {
					BigGlobeScriptedChunkGenerator generator = getGenerator(context.getSource());
					if (generator != null) {
						generator.setDisplay(context.getArgument("filter", String.class));
						return 1;
					}
					else {
						return 0;
					}
				})
			)
		);
	}

	public static @Nullable BigGlobeScriptedChunkGenerator getGenerator(FabricClientCommandSource source) {
		Minecraft client = source.getClient();
		if (client.getSingleplayerServer() == null || client.level == null) return null;
		ServerLevel world = client.getSingleplayerServer().getLevel(client.level.dimension());
		if (world == null) return null;
		return world.getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}
}