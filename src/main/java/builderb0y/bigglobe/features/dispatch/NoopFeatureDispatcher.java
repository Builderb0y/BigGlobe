package builderb0y.bigglobe.features.dispatch;

import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

@RecordLike({})
public class NoopFeatureDispatcher implements FeatureDispatcher {

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, RegistryEntry<FeatureDispatcher> selfEntry) {

	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}
}