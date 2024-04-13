package builderb0y.bigglobe.features;

import java.util.Arrays;
import java.util.Comparator;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.noise.Permuter;
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

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface FeatureDispatcher extends Script {

	public abstract void generate(WorldWrapper world, RandomGenerator random);

	public static class FeatureDispatchers {

		public final TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers;
		public transient ConfiguredRockReplacerFeature<?> @Nullable [] flattenedRockReplacers;
		public final RegistryEntry<FeatureDispatcher.Holder>[] raw, normal;

		public FeatureDispatchers(
			TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers,
			RegistryEntry<FeatureDispatcher.Holder>[] raw,
			RegistryEntry<FeatureDispatcher.Holder>[] normal
		) {
			this.rock_replacers = rock_replacers;
			this.raw = raw;
			this.normal = normal;
		}

		public void generateRaw(WorldWrapper world) {
			long chunkSeed = Permuter.permute(
				world.seed() ^ 0x8938ECF5EEA7B9B2L,
				minModifiableX(world) >> 4,
				minModifiableY(world) >> 4,
				minModifiableZ(world) >> 4
			);
			Permuter permuter = new Permuter(0L);
			for (RegistryEntry<FeatureDispatcher.Holder> entry : this.raw) {
				permuter.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(entry).hashCode()));
				entry.value().generate(world, permuter);
			}
		}

		public void generateNormal(WorldWrapper world) {
			long chunkSeed = Permuter.permute(
				world.seed() ^ 0xDD59B178ABC90AC1L,
				minModifiableX(world) >> 4,
				minModifiableY(world) >> 4,
				minModifiableZ(world) >> 4
			);
			Permuter permuter = new Permuter(0L);
			for (RegistryEntry<FeatureDispatcher.Holder> entry : this.normal) {
				permuter.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(entry).hashCode()));
				entry.value().generate(world, permuter);
			}
		}

		public ConfiguredRockReplacerFeature<?> @NotNull [] getFlattenedRockReplacers() {
			if (this.flattenedRockReplacers == null) {
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
			return this.flattenedRockReplacers;
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

	@AddPseudoField("dispatcher")
	public static class Holder extends ScriptHolder<FeatureDispatcher> implements FeatureDispatcher {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public Holder(ScriptUsage dispatcher) throws ScriptParsingException {
			super(dispatcher);
		}

		public ScriptUsage dispatcher() {
			return this.usage;
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(FeatureDispatcher.class, this.usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
				.addEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
				.addEnvironment(NbtScriptEnvironment.createMutable())
				.addEnvironment(RandomScriptEnvironment.create(load("random", type(RandomGenerator.class))))
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
		public void generate(WorldWrapper world, RandomGenerator random) {
			try {
				this.script.generate(world, random);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception generating features in area " + world.coordination.mutableArea(), throwable);
			}
		}
	}
}