package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

public interface DependencyView {

	public abstract Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies();
}