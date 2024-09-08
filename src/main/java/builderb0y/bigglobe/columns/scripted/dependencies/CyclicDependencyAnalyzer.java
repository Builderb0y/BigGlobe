package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.traits.WorldTraits;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class CyclicDependencyAnalyzer implements Consumer<RegistryEntry<? extends DependencyView>> {

	public final WorldTraits traits;
	public final Set<RegistryEntry<? extends DependencyView>>
		seen  = new ObjectOpenCustomHashSet<>(256, DependencyDepthSorter.REGISTRY_ENTRY_STRATEGY),
		stack = new ObjectLinkedOpenCustomHashSet<>(16, DependencyDepthSorter.REGISTRY_ENTRY_STRATEGY);

	public CyclicDependencyAnalyzer(WorldTraits traits) {
		this.traits = traits;
	}

	@Override
	public void accept(RegistryEntry<? extends DependencyView> entry) {
		if (!this.stack.add(entry)) {
			throw new CyclicDependencyException(
				Stream
				.concat(
					this
					.stack
					.stream()
					.dropWhile((RegistryEntry<? extends DependencyView> compare) -> compare != entry),
					Stream.of(entry)
				)
				.map(UnregisteredObjectException::getKey)
				.map(DependencyDepthSorter::keyToString)
				.collect(Collectors.joining(" -> "))
			);
		}
		try {
			if (this.seen.add(entry)) {
				entry.value().streamDirectDependencies(entry, this.traits).forEach(this);
			}
		}
		finally {
			this.stack.remove(entry);
		}
	}
}