package builderb0y.bigglobe.features.dispatch;

import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

public class GroupFeatureDispatcher implements FeatureDispatcher {

	public final Holder<FeatureDispatcher>[] dispatchers;

	public GroupFeatureDispatcher(Holder<FeatureDispatcher>[] dispatchers) {
		this.dispatchers = dispatchers;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Arrays.stream(this.dispatchers);
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, Holder<FeatureDispatcher> selfEntry) {
		for (Holder<FeatureDispatcher> dispatcher : this.dispatchers) {
			dispatcher.value().generate(world, random, chunkSeed, dispatcher);
		}
	}
}