package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.cortex.voxy.client.core.IGetVoxelCore;
import me.cortex.voxy.common.world.WorldEngine;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.compat.voxy.AbstractVoxyWorldGenerator;
import builderb0y.bigglobe.compat.voxy.DebugVoxyWorldGenerator;
import builderb0y.bigglobe.mixinInterfaces.VoxyGeneratorHolder;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;

public class VoxyDebugCommand {

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		dispatcher.register(
			ClientCommandManager
			.literal(BigGlobeMod.MODID + ":voxyDebug")
			.requires((FabricClientCommandSource source) -> DisplayColumnsClientCommand.getGenerator(source) != null)
			.executes((CommandContext<FabricClientCommandSource> context) -> {
				((IGetVoxelCore)(MinecraftClient.getInstance().worldRenderer)).reloadVoxelCore();
				return 1;
			})
			.then(
				ClientCommandManager
				.argument("state", BlockStateArgumentType.blockState(registryAccess))
				.executes((CommandContext<FabricClientCommandSource> context) -> {
					IGetVoxelCore coreGetter = (IGetVoxelCore)(MinecraftClient.getInstance().worldRenderer);
					if (((VoxyGeneratorHolder)(coreGetter.getVoxelCore().getWorldEngine())).bigglobe_getVoxyGenerator() instanceof DebugVoxyWorldGenerator oldGenerator) {
						Object2ObjectArrayMap<BlockState, ColumnToIntScript.Holder> newStates = new Object2ObjectArrayMap<>(oldGenerator.states);
						if (newStates.remove(context.getArgument("state", BlockStateArgument.class).getBlockState()) != null) {
							AbstractVoxyWorldGenerator.reloadWith(
								(WorldEngine newEngine, ServerWorld world, BigGlobeScriptedChunkGenerator chunkGenerator) -> {
									return new DebugVoxyWorldGenerator(newEngine, world, chunkGenerator, newStates);
								},
								coreGetter
							);
							return 1;
						}
					}
					return 0;
				})
				.then(
					ClientCommandManager
					.argument("script", StringArgumentType.greedyString())
					.executes((CommandContext<FabricClientCommandSource> context) -> {
						IGetVoxelCore coreGetter = (IGetVoxelCore)(MinecraftClient.getInstance().worldRenderer);
						Object2ObjectArrayMap<BlockState, ColumnToIntScript.Holder> newStates;
						if (((VoxyGeneratorHolder)(coreGetter.getVoxelCore().getWorldEngine())).bigglobe_getVoxyGenerator() instanceof DebugVoxyWorldGenerator oldGenerator) {
							newStates = new Object2ObjectArrayMap<>(oldGenerator.states.size() + 1);
							newStates.putAll(oldGenerator.states);
						}
						else {
							newStates = new Object2ObjectArrayMap<>(1);
						}
						ColumnToIntScript.Holder script = new ColumnToIntScript.Holder(new ScriptUsage(context.getArgument("script", String.class)));
						if (!compile(script, context.getSource())) return 0;
						newStates.put(context.getArgument("state", BlockStateArgument.class).getBlockState(), script);

						AbstractVoxyWorldGenerator.reloadWith(
							(WorldEngine engine, ServerWorld world, BigGlobeScriptedChunkGenerator chunkGenerator) -> {
								return new DebugVoxyWorldGenerator(engine, world, chunkGenerator, newStates);
							},
							coreGetter
						);
						return 1;
					})
				)
			)
		);
	}

	public static boolean compile(ScriptHolder<?> script, FabricClientCommandSource source) {
		try {
			script.compile(
				DisplayColumnsClientCommand
				.getGenerator(source)
				.columnEntryRegistry
			);
			return true;
		}
		catch (ScriptParsingException exception) {
			exception.getLocalizedMessage().lines().map(Text::literal).forEachOrdered(source::sendError);
			return false;
		}
	}
}