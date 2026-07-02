package builderb0y.bigglobe.features.dispatch;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.Alias;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.ReadOnlyWorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.ScriptUsage;

public class ScriptedFeatureDispatcher implements FeatureDispatcher {

	public final @Alias("script") Catcher dispatcher;

	public ScriptedFeatureDispatcher(Catcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, Holder<FeatureDispatcher> selfEntry) {
		random.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(selfEntry).hashCode()));
		this.dispatcher.generate(world, random);
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.dispatcher.usage.streamDirectDependencies();
	}

	public static interface ScriptedFeatureDispatcherImpl extends Script {

		public abstract void generate(WorldWrapper world, RandomGenerator random);
	}

	@Wrapper
	public static class Catcher extends ScriptCatcher<ScriptedFeatureDispatcherImpl> implements ScriptedFeatureDispatcherImpl {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Catcher(ScriptUsage usage) throws ScriptParsingException {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(ScriptedFeatureDispatcherImpl.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.configureEnvironment(NbtScriptEnvironment.createMutable())
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configureEnvironment(GridScriptEnvironment.createWithSeed(ReadOnlyWorldWrapper.INFO.seed(WORLD.loadSelf)))
				.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
				.configure((ExpressionParser parser) -> {
					MutableScriptEnvironment environment = parser.environment.mutable();
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
						environment.addVariable(Handlers.methodBuilder(FeatureDispatcher.class, name).addImplicitArgument(WORLD.loadSelf).buildVariable());
					}
					registry.setupEnvironment(
						parser,
						new ExternalEnvironmentParams()
						.withLookup("world", WORLD.loadSelf)
					);
				})
				.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
				.addImportedValue("random", ReadOnlyWorldWrapper.INFO.random(WORLD.loadSelf))
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public void generate(WorldWrapper world, RandomGenerator random) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.generate(world, random);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
			finally {
				manager.used = used;
			}
		}
	}
}