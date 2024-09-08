package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;

public interface DependencyView {

	public abstract Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies(RegistryEntry<? extends DependencyView> self, WorldTraits traits);

	public static interface SimpleDependencyView extends DependencyView {

		public abstract Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies();

		@Override
		public default Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies(RegistryEntry<? extends DependencyView> self, WorldTraits traits) {
			return this.streamDirectDependencies();
		}
	}

	public static interface MutableDependencyView extends DependencyView {

		public abstract void addDependency(RegistryEntry<? extends DependencyView> entry);
	}

	public static interface SetBasedMutableDependencyView extends MutableDependencyView, SimpleDependencyView {

		public abstract Set<RegistryEntry<? extends DependencyView>> getDependencies();

		@Override
		public default void addDependency(RegistryEntry<? extends DependencyView> entry) {
			this.getDependencies().add(entry);
		}

		@Override
		public default Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
			return this.getDependencies().stream();
		}
	}
}