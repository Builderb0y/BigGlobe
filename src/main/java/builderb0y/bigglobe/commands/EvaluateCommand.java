package builderb0y.bigglobe.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.CommandVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
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
						.executes((CommandContext<CommandSourceStack> context) -> {
							CommandScript.Holder script = new CommandScript.Holder(context.getArgument("script", String.class));
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
							WorldWrapper world = new WorldWrapper(
								new WorldDelegator(actualWorld),
								generator,
								Permuter.from(actualWorld.random),
								new Coordination(SymmetricOffset.IDENTITY, area, area),
								ColumnUsage.GENERIC.normalHints()
							);
							Object result = script.evaluate(
								world,
								BigGlobeMath.floorI(position.x),
								BigGlobeMath.floorI(position.y),
								BigGlobeMath.floorI(position.z)
							);
							if (result instanceof Throwable) {
								context.getSource().sendFailure(Component.literal(" = " + result + "; check your logs for more info."));
							}
							else {
								context.getSource().sendSuccess(() -> Component.literal(" = " + result), false);
							}
							return result instanceof Number number ? number.intValue() : 1;
						})
				)
		);
	}

	public static @Nullable BigGlobeScriptedChunkGenerator getGenerator(CommandSourceStack source) {
		return source.getLevel().getChunkSource().getGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator : null;
	}

	public static interface CommandScript extends Script {

		public abstract Object evaluate(WorldWrapper world, int originX, int originY, int originZ);

		public static class Holder extends ScriptHolder<CommandScript> implements CommandScript {

			public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

			public Holder(String source) {
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
						.configureEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
						.addEnvironment(MathScriptEnvironment.INSTANCE)
						.configureEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
						.configureEnvironment(SymmetryScriptEnvironment.create(WORLD.random))
						.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
						.configureEnvironment(NbtScriptEnvironment.createMutable())
						.configureEnvironment(WoodPaletteScriptEnvironment.create(WORLD.random))
						.configureEnvironment(RandomScriptEnvironment.create(WORLD.random))
						.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
						.configureEnvironment(GridScriptEnvironment.createWithSeed(WORLD.seed))
						.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
						.configureEnvironment((MutableScriptEnvironment environment) -> {
							environment
								.addVariableLoad("originX", TypeInfos.INT)
								.addVariableLoad("originY", TypeInfos.INT)
								.addVariableLoad("originZ", TypeInfos.INT);
							registry.setupExternalEnvironment(
								environment,
								new ExternalEnvironmentParams()
									.withLookup(WORLD.loadSelf)
									.withXZ(
										load("originX", TypeInfos.INT),
										load("originZ", TypeInfos.INT)
									)
									.withY(load("originY", TypeInfos.INT))
							);
						})
						.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
						.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public Object evaluate(WorldWrapper world, int originX, int originY, int originZ) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.evaluate(world, originX, originY, originZ);
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