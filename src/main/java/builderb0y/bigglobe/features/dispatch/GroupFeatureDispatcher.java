package builderb0y.bigglobe.features.dispatch;

import java.util.Arrays;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public class GroupFeatureDispatcher implements FeatureDispatcher {

	public final RegistryEntry<FeatureDispatcher>[] dispatchers;

	public GroupFeatureDispatcher(RegistryEntry<FeatureDispatcher>[] dispatchers) {
		this.dispatchers = dispatchers;
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return Arrays.stream(this.dispatchers);
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, RegistryEntry<FeatureDispatcher> selfEntry) {
		for (RegistryEntry<FeatureDispatcher> dispatcher : this.dispatchers) {
			dispatcher.value().generate(world, random, chunkSeed, dispatcher);
		}
	}
}