package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;

public interface ColumnValueDependencyHolder {

	public default void addDependency(RegistryEntry<ColumnEntry> entry) {
		this.getDependencies().add(entry);
	}

	public abstract Set<RegistryEntry<ColumnEntry>> getDependencies();
}