package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Vec3d;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.commands.CommandScript.LazyCommandScript;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;

public class EvaluateCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager
			.literal(BigGlobeMod.MODID + ":evaluate")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				CommandManager
				.argument("script", new CommandScriptArgument())
				.executes(context -> {
					CommandScript script = context.getArgument("script", LazyCommandScript.class);
					try {
						WorldWrapper world = new WorldWrapper(
							context.getSource().getWorld(),
							Permuter.from(context.getSource().getWorld().random),
							new Coordination(0, 0, BlockRotation.NONE, BlockBox.infinite())
						);
						Vec3d position = context.getSource().getPosition();
						WorldColumn column = WorldColumn.forWorld(
							context.getSource().getWorld(),
							BigGlobeMath.floorI(position.x),
							BigGlobeMath.floorI(position.z)
						);
						Object result = script.evaluate(world, column, column.x, BigGlobeMath.floorI(position.y), column.z);
						context.getSource().sendFeedback(Text.literal(" = " + result), false);
						return result instanceof Number number ? number.intValue() : 1;
					}
					catch (Throwable throwable) {
						context.getSource().sendError(Text.literal(throwable.toString()));
						return 0;
					}
				})
			)
		);
	}
}