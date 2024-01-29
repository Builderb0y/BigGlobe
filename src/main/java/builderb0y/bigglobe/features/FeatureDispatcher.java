package builderb0y.bigglobe.features;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

public interface FeatureDispatcher extends Script {

	public abstract void generate(WorldWrapper world);

	public static class DualFeatureDispatcher {
		public final Holder raw, normal;

		public DualFeatureDispatcher(Holder raw, Holder normal) {
			this.raw = raw;
			this.normal = normal;
		}
	}

	public static int minX(WorldWrapper world) { return world.coordination.mutableArea().getMinX(); }
	public static int minY(WorldWrapper world) { return world.coordination.mutableArea().getMinY(); }
	public static int minZ(WorldWrapper world) { return world.coordination.mutableArea().getMinZ(); }
	public static int maxX(WorldWrapper world) { return world.coordination.mutableArea().getMaxX(); }
	public static int maxY(WorldWrapper world) { return world.coordination.mutableArea().getMaxY(); }
	public static int maxZ(WorldWrapper world) { return world.coordination.mutableArea().getMaxZ(); }

	public static class Holder extends ScriptHolder<FeatureDispatcher> implements FeatureDispatcher {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(FeatureDispatcher.class, this.usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
				.addEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(RandomScriptEnvironment.create(WORLD.random))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					registry.setupExternalEnvironmentWithLookup(
						environment
						.addVariableLoad("originX", TypeInfos.INT)
						.addVariableLoad("originY", TypeInfos.INT)
						.addVariableLoad("originZ", TypeInfos.INT)
						.addVariable("distantHorizons", WORLD.distantHorizons),
						WORLD.loadSelf
					);
				})
				.parse()
			);
		}

		@Override
		public void generate(WorldWrapper world) {
			try {
				this.script.generate(world);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception generating features in area " + world.coordination.mutableArea(), throwable);
			}
		}
	}
}