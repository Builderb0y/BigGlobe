package builderb0y.bigglobe.features;

import java.util.Arrays;
import java.util.Comparator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;

public interface FeatureDispatcher extends Script {

	public abstract void generate(WorldWrapper world);

	public static class DualFeatureDispatcher {

		public final TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers;
		public final transient ConfiguredRockReplacerFeature<?>[] flattenedRockReplacers;
		public final Holder raw, normal;

		public DualFeatureDispatcher(
			TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers,
			Holder raw,
			Holder normal
		) {
			this.rock_replacers = rock_replacers;
			this.raw = raw;
			this.normal = normal;
			this.flattenedRockReplacers = (
				Arrays
				.stream(rock_replacers)
				.flatMap((TagOrObject<ConfiguredFeature<?, ?>> tagOrObject) -> {
					return tagOrObject.stream().sorted(Comparator.comparing(UnregisteredObjectException::getID));
				})
				.filter((RegistryEntry<ConfiguredFeature<?, ?>> entry) -> {
					//entry.value().feature().is(RockReplacerFeature).unless(
					//	log warning
					//)
					if (entry.value().feature() instanceof RockReplacerFeature<?>) {
						return true;
					}
					else {
						BigGlobeMod.LOGGER.warn("Feature dispatcher specified " + UnregisteredObjectException.getID(entry) + " as a rock replacer, but that configured feature is not a rock replacer. It will be ignored.");
						return false;
					}
				})
				.map(RegistryEntry::value)
				.map(ConfiguredRockReplacerFeature::new)
				.toArray(ConfiguredRockReplacerFeature[]::new)
			);
		}
	}

	public static int minModifiableX(WorldWrapper world) { return world.coordination.mutableArea().getMinX(); }
	public static int minModifiableY(WorldWrapper world) { return world.coordination.mutableArea().getMinY(); }
	public static int minModifiableZ(WorldWrapper world) { return world.coordination.mutableArea().getMinZ(); }
	public static int maxModifiableX(WorldWrapper world) { return world.coordination.mutableArea().getMaxX(); }
	public static int maxModifiableY(WorldWrapper world) { return world.coordination.mutableArea().getMaxY(); }
	public static int maxModifiableZ(WorldWrapper world) { return world.coordination.mutableArea().getMaxZ(); }
	public static int minAccessibleX(WorldWrapper world) { return world.coordination.immutableArea().getMinX(); }
	public static int minAccessibleY(WorldWrapper world) { return world.coordination.immutableArea().getMinY(); }
	public static int minAccessibleZ(WorldWrapper world) { return world.coordination.immutableArea().getMinZ(); }
	public static int maxAccessibleX(WorldWrapper world) { return world.coordination.immutableArea().getMaxX(); }
	public static int maxAccessibleY(WorldWrapper world) { return world.coordination.immutableArea().getMaxY(); }
	public static int maxAccessibleZ(WorldWrapper world) { return world.coordination.immutableArea().getMaxZ(); }

	@Wrapper
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