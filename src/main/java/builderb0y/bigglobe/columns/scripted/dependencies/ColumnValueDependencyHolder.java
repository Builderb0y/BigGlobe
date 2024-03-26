package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

public interface ColumnValueDependencyHolder {

	public default void addDependency(RegistryEntry<? extends ColumnValueDependencyHolder> entry) {
		this.getDependencies().add(entry);
	}

	public abstract Set<RegistryEntry<? extends ColumnValueDependencyHolder>> getDependencies();
}