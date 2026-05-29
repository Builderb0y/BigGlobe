package builderb0y.bigglobe.features.dispatch;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.DelayedEntryList;

public class TagFeatureDispatcher implements FeatureDispatcher {

	public final DelayedEntryList<FeatureDispatcher> tag;

	public TagFeatureDispatcher(DelayedEntryList<FeatureDispatcher> tag) {
		this.tag = tag;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return this.tag.entryStream();
	}

	@Override
	public void generate(WorldWrapper world, Permuter random, long chunkSeed, Holder<FeatureDispatcher> selfEntry) {
		for (Holder<FeatureDispatcher> entry : this.tag.entryList()) {
			entry.value().generate(world, random, chunkSeed, entry);
		}
	}
}