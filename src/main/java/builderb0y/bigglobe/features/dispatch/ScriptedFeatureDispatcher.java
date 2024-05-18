package builderb0y.bigglobe.features.dispatch;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedFeatureDispatcher implements FeatureDispatcher {

	public final Holder dispatcher;

	public ScriptedFeatureDispatcher(Holder dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, RegistryEntry<FeatureDispatcher> selfEntry) {
		random.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(selfEntry).hashCode()));
		this.dispatcher.generate(world, random);
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}

	public static interface ScriptedFeatureDispatcherImpl extends Script {

		public abstract void generate(WorldWrapper world, RandomGenerator random);
	}

	@Wrapper
	public static class Holder extends ScriptHolder<ScriptedFeatureDispatcherImpl> implements ScriptedFeatureDispatcherImpl {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Holder(ScriptUsage usage) throws ScriptParsingException {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ScriptedFeatureDispatcherImpl.class, this.usage)
				.configureEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
				.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.configureEnvironment(RandomScriptEnvironment.create(load("random", type(RandomGenerator.class))))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(WORLD.seed))
				.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment((MutableScriptEnvironment environment) -> {
					for (String name : new String[] {
						"minModifiableX",
						"minModifiableY",
						"minModifiableZ",
						"maxModifiableX",
						"maxModifiableY",
						"maxModifiableZ",
						"minAccessibleX",
						"minAccessibleY",
						"minAccessibleZ",
						"maxAccessibleX",
						"maxAccessibleY",
						"maxAccessibleZ",
					}) {
						environment.addVariable(name, Handlers.builder(FeatureDispatcher.class, name).addImplicitArgument(WORLD.loadSelf).buildVariable());
					}
					environment.addVariable("distantHorizons", WORLD.distantHorizons);
					registry.setupExternalEnvironment(environment, new ExternalEnvironmentParams().withLookup(WORLD.loadSelf));
				})
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void generate(WorldWrapper world, RandomGenerator random) {
			NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.generate(world, random);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception generating features in area " + world.coordination.mutableArea(), throwable);
			}
			finally {
				manager.used = used;
			}
		}
	}
}