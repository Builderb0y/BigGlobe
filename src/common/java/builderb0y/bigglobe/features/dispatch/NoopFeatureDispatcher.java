package builderb0y.bigglobe.features.dispatch;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.RecordLike;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

@RecordLike({})
public class NoopFeatureDispatcher implements FeatureDispatcher {

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, Holder<FeatureDispatcher> selfEntry) {}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}
}