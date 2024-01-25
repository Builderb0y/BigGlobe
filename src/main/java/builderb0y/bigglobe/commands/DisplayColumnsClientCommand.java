package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;

@Environment(EnvType.CLIENT)
public class DisplayColumnsClientCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommandManager
			.literal(BigGlobeMod.MODID + ":displayColumns")
			.requires((FabricClientCommandSource source) -> getGenerator() != null)
			.executes((CommandContext<FabricClientCommandSource> context) -> {
				BigGlobeScriptedChunkGenerator generator = getGenerator();
				if (generator != null) {
					generator.setDisplay(null);
					return 1;
				}
				else {
					return 0;
				}
			})
			.then(
				ClientCommandManager
				.argument("filter", StringArgumentType.greedyString())
				.executes((CommandContext<FabricClientCommandSource> context) -> {
					BigGlobeScriptedChunkGenerator generator = getGenerator();
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

	public static @Nullable BigGlobeScriptedChunkGenerator getGenerator() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getServer() == null || client.world == null) return null;
		ServerWorld world = client.getServer().getWorld(client.world.getRegistryKey());
		if (world == null) return null;
		return world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator displayer ? displayer : null;
	}

	public static @Nullable ScriptedColumn getColumn(int x, int z) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getServer() == null || client.world == null) return null;
		ServerWorld world = client.getServer().getWorld(client.world.getRegistryKey());
		if (world == null) return null;
		if (world.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			return generator.columnEntryRegistry.columnFactory.create(generator.seed, x, z, generator.height.min_y(), generator.height.max_y());
		}
		else {
			return null;
		}
	}
}