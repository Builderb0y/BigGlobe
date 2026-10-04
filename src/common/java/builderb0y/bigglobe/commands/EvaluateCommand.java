package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.ReadOnlyWorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.CommandVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.SourceScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EvaluateCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands
			.literal(BigGlobeMod.MODID + ":evaluate")
			.requires(CommandVersions.levelPredicate(4).and((CommandSourceStack source) -> getGenerator(source) != null))
			.then(
				Commands
				.argument("script", StringArgumentType.greedyString())
				.executes(EvaluateCommand::eval)
			)
		);
	}

	public static int eval(CommandContext<CommandSourceStack> context) {
		return eval(context, context.getArgument("script", String.class));
	}

	public static int eval(CommandContext<CommandSourceStack> context, String source) {
		CommandScript.Catcher script = new CommandScript.Catcher(source);
		if (!BigGlobeLocateCommand.compile(script, context.getSource())) return 0;
		BigGlobeScriptedChunkGenerator generator = getGenerator(context.getSource());
		ServerLevel actualWorld = context.getSource().getLevel();
		Vec3 position = context.getSource().getPosition();
		BoundingBox area = new BoundingBox(
			-30_000_000,
			HeightLimitViewVersions.getMinY(actualWorld),
			-30_000_000,
			+30_000_000,
			HeightLimitViewVersions.getMaxY(actualWorld),
			+30_000_000
		);
		int originX = BigGlobeMath.floorI(position.x);
		int originY = BigGlobeMath.floorI(position.y);
		int originZ = BigGlobeMath.floorI(position.z);
		WorldWrapper world = new WorldWrapper(
			new WorldDelegator(actualWorld),
			generator,
			Permuter.from(actualWorld.getRandom()),
			new Coordination(SymmetricOffset.IDENTITY.offset(originX, originY, originZ), area, area),
			ColumnUsage.GENERIC.normalHints()
		);
		Object result = script.evaluate(world);
		if (result instanceof Throwable) {
			context.getSource().sendFailure(Component.literal(" = " + result + "; check your logs for more info."));
		}
		else {
			context.getSource().sendSuccess(() -> Component.literal(" = " + result), false);
		}
		return result instanceof Number number ? number.intValue() : 1;
	}

	public static @Nullable BigGlobeScriptedChunkGenerator getGenerator(CommandSourceStack source) {
		return source.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}

	public static interface CommandScript extends Script {

		public abstract Object evaluate(WorldWrapper world);

		public static class Catcher extends ScriptCatcher<CommandScript> implements CommandScript {

			public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

			public Catcher(String source) {
				super(new SourceScriptUsage(source));
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new ScriptParser<>(CommandScript.class, this.usage.getSource(), null, 0) {

						@Override
						public InsnTree createReturn(InsnTree value) {
							if (value.getTypeInfo().isVoid()) return return_(seq(value, ldc(null, TypeInfos.OBJECT)));
							else return return_(value.cast(this, TypeInfos.OBJECT, CastMode.EXPLICIT_THROW, false));
						}
					}
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.configureEnvironment(SymmetryScriptEnvironment.create(ReadOnlyWorldWrapper.INFO.random(WORLD.loadSelf)))
					.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
					.configureEnvironment(NbtScriptEnvironment.createMutable())
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment(GridScriptEnvironment.createWithSeed(ReadOnlyWorldWrapper.INFO.seed(WORLD.loadSelf)))
					.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
					.configure((ExpressionParser parser) -> {
						registry.setupEnvironment(
							parser,
							new ExternalEnvironmentParams()
							.withLookup("world", WORLD.loadSelf)
							.withXZ(WORLD.originX, WORLD.originZ)
							.withY(WORLD.originY)
						);
					})
					.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
					.addImportedValue("random", ReadOnlyWorldWrapper.INFO.random(WORLD.loadSelf))
					.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public Object evaluate(WorldWrapper world) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.evaluate(world);
				}
				catch (Throwable throwable) {
					ScriptLogger.LOGGER.error("Caught exception from CommandScript:", throwable);
					ScriptLogger.LOGGER.error("Script source was:\n" + ScriptLogger.addLineNumbers(this.getSource()));
					return throwable;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}
}