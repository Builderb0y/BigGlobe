package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.HashSet;
import java.util.function.Consumer;

import net.minecraft.registry.entry.RegistryEntry;

public class IndirectDependencyCollector extends HashSet<RegistryEntry<? extends DependencyView>> implements Consumer<RegistryEntry<? extends DependencyView>> {

	@Override
	public void accept(RegistryEntry<? extends DependencyView> entry) {
		if (this.add(entry)) {
			entry.value().streamDirectDependencies().forEach(this);
		}
	}
}