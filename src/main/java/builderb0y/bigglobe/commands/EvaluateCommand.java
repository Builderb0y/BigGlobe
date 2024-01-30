package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.commands.CommandScript.LazyCommandScript;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.ServerCommandSourceVersions;

public class EvaluateCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":evaluate")
			.requires((ServerCommandSource source) -> source.hasPermissionLevel(4) && getGenerator(source) != null)
			.then(
				CommandManager
				.argument("script", new CommandScriptArgument())
				.executes((CommandContext<ServerCommandSource> context) -> {
					LazyCommandScript script = context.getArgument("script", LazyCommandScript.class);
					ServerWorld actualWorld = context.getSource().getWorld();
					BigGlobeScriptedChunkGenerator generator = getGenerator(context.getSource());
					Vec3d position = context.getSource().getPosition();
					WorldWrapper world = new WorldWrapper(
						new WorldDelegator(actualWorld),
						generator,
						Permuter.from(actualWorld.random),
						new Coordination(SymmetricOffset.IDENTITY, BlockBox.infinite(), BlockBox.infinite()),
						false
					);
					WorldColumn column = WorldColumn.forWorld(
						actualWorld,
						BigGlobeMath.floorI(position.x),
						BigGlobeMath.floorI(position.z)
					);
					Object result = script.evaluate(world, column, column.x, BigGlobeMath.floorI(position.y), column.z);
					if (result instanceof Throwable) {
						context.getSource().sendError(Text.literal(" = " + result + "; check your logs for more info."));
					}
					else {
						ServerCommandSourceVersions.sendFeedback(context.getSource(), () -> Text.literal(" = " + result), false);
					}
					return result instanceof Number number ? number.intValue() : 1;
				})
			)
		);
	}

	public static @Nullable BigGlobeScriptedChunkGenerator getGenerator(ServerCommandSource source) {
		return source.getWorld().getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}
}