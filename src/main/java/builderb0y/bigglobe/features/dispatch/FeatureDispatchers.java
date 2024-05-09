package builderb0y.bigglobe.features.dispatch;

import java.util.Arrays;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyAnalyzer;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyException;
import builderb0y.bigglobe.features.RockReplacerFeature;
import builderb0y.bigglobe.features.RockReplacerFeature.ConfiguredRockReplacerFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.TagOrObject;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@UseVerifier(name = "verify", in = FeatureDispatchers.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class FeatureDispatchers {

	public final TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers;
	public transient ConfiguredRockReplacerFeature<?> @Nullable [] flattenedRockReplacers;
	public final RegistryEntry<FeatureDispatcher> raw, normal;

	public FeatureDispatchers(
		TagOrObject<ConfiguredFeature<?, ?>>[] rock_replacers,
		RegistryEntry<FeatureDispatcher> raw,
		RegistryEntry<FeatureDispatcher> normal
	) {
		this.rock_replacers = rock_replacers;
		this.raw = raw;
		this.normal = normal;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, FeatureDispatchers> context) throws VerifyException {
		FeatureDispatchers dispatchers = context.object;
		if (dispatchers == null) return;
		try {
			new CyclicDependencyAnalyzer().accept(dispatchers.raw);
			new CyclicDependencyAnalyzer().accept(dispatchers.normal);
		}
		catch (CyclicDependencyException exception) {
			throw new VerifyException(() -> "Cyclic feature dispatcher dependency cycle: " + exception.getMessage(), exception);
		}
	}

	public void generateRaw(WorldWrapper world) {
		long chunkSeed = Permuter.permute(
			world.seed() ^ 0x8938ECF5EEA7B9B2L,
			FeatureDispatcher.minModifiableX(world) >> 4,
			FeatureDispatcher.minModifiableY(world) >> 4,
			FeatureDispatcher.minModifiableZ(world) >> 4
		);
		this.raw.value().generate(world, new Permuter(0L), chunkSeed, this.raw);
	}

	public void generateNormal(WorldWrapper world) {
		long chunkSeed = Permuter.permute(
			world.seed() ^ 0xDD59B178ABC90AC1L,
			FeatureDispatcher.minModifiableX(world) >> 4,
			FeatureDispatcher.minModifiableY(world) >> 4,
			FeatureDispatcher.minModifiableZ(world) >> 4
		);
		this.normal.value().generate(world, new Permuter(0L), chunkSeed, this.normal);
	}

	public ConfiguredRockReplacerFeature<?> @NotNull [] getFlattenedRockReplacers() {
		if (this.flattenedRockReplacers == null) {
			this.flattenedRockReplacers = (
				Arrays
				.stream(this.rock_replacers)
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