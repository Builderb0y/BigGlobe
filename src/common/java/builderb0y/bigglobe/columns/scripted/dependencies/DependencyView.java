package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;

public interface DependencyView {

	public abstract Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies(Holder<? extends DependencyView> self, WorldTraits traits);

	public static interface SimpleDependencyView extends DependencyView {

		public abstract Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies();

		@Override
		public default Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies(Holder<? extends DependencyView> self, WorldTraits traits) {
			return this.streamDirectDependencies();
		}
	}

	public static interface EmptyDependencyView extends SimpleDependencyView {

		@Override
		public default Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Stream.empty();
		}
	}

	public static interface MutableDependencyView extends DependencyView {

		public abstract void addDependency(Holder<? extends DependencyView> entry);

		public default void addAllDependencies(SimpleDependencyView dependency) {
			dependency.streamDirectDependencies().forEach(this::addDependency);
		}
	}

	public static interface SetBasedMutableDependencyView extends MutableDependencyView, SimpleDependencyView {

		public abstract Set<Holder<? extends DependencyView>> getDependencies();

		public static SetBasedMutableDependencyView from(Set<Holder<? extends DependencyView>> dependencies) {
			return () -> dependencies;
		}

		@Override
		public default void addDependency(Holder<? extends DependencyView> entry) {
			this.getDependencies().add(entry);
		}

		@Override
		public default Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return this.getDependencies().stream();
		}
	}
}