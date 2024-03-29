package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

public interface MutableDependencyView extends DependencyView {

	public default void addDependency(RegistryEntry<? extends DependencyView> entry) {
		this.getDependencies().add(entry);
	}

	public abstract Set<RegistryEntry<? extends DependencyView>> getDependencies();

	@Override
	public default Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		return this.getDependencies().stream();
	}
}