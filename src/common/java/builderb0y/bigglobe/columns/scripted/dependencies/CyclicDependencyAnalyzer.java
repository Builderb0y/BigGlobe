package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class CyclicDependencyAnalyzer extends GenericCyclicDependencyAnalyzer<Holder<? extends DependencyView>> {

	public final WorldTraits traits;

	public CyclicDependencyAnalyzer(WorldTraits traits) {
		this.traits = traits;
	}

	@Override
	public boolean areCyclesFatal(Holder<? extends DependencyView> node) {
		return node.value() instanceof ColumnEntry;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> getDependencies(Holder<? extends DependencyView> object) {
		return object.value().streamDirectDependencies(object, this.traits);
	}

	@Override
	public String format(Holder<? extends DependencyView> object) {
		return UnregisteredObjectException.getKey(object).toString();
	}
}