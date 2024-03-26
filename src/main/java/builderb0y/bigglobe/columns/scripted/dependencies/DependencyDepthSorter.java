package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.*;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class DependencyDepthSorter {

	public static final Comparator<RegistryEntry<? extends ColumnValueDependencyHolder>> REGISTRY_ENTRY_COMPARATOR = Comparator.comparing(UnregisteredObjectException::getID);
	public static final Hash.Strategy<RegistryEntry<?>> REGISTRY_ENTRY_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), UnregisteredObjectException::getKey);

	public final List<TreeSet<RegistryEntry<? extends ColumnValueDependencyHolder>>> results = new ArrayList<>(16);
	public final Object2IntOpenCustomHashMap<RegistryEntry<? extends ColumnValueDependencyHolder>> cache = new Object2IntOpenCustomHashMap<>(256, REGISTRY_ENTRY_STRATEGY);
	{ this.cache.defaultReturnValue(-1); }
	public final ObjectLinkedOpenCustomHashSet<RegistryEntry<? extends ColumnValueDependencyHolder>> cyclicDetector = new ObjectLinkedOpenCustomHashSet<>(16, REGISTRY_ENTRY_STRATEGY);

	public int recursiveComputeDepth(RegistryEntry<? extends ColumnValueDependencyHolder> entry) {
		if (!this.cyclicDetector.add(entry)) {
			throw new CyclicColumnValueDependencyException(
				Stream.concat(
					this
					.cyclicDetector
					.stream()
					.dropWhile((RegistryEntry<? extends ColumnValueDependencyHolder> compare) -> compare != entry),
					Stream.of(entry)
				)
				.map(UnregisteredObjectException::getID)
				.map(Identifier::toString)
				.collect(Collectors.joining(" -> "))
			);
		}
		try {
			int depth = this.cache.getInt(entry);
			if (depth < 0) {
				depth = 0;
				Set<RegistryEntry<? extends ColumnValueDependencyHolder>> dependencies = entry.value().getDependencies();
				if (!dependencies.isEmpty()) {
					for (RegistryEntry<? extends ColumnValueDependencyHolder> dependency : dependencies) {
						depth = Math.max(depth, this.recursiveComputeDepth(dependency));
					}
					depth++;
				}
				this.cache.put(entry, depth);
				while (this.results.size() <= depth) {
					this.results.add(new TreeSet<>(REGISTRY_ENTRY_COMPARATOR));
				}
				this.results.get(depth).add(entry);
			}
			return depth;
		}
		finally {
			this.cyclicDetector.remove(entry);
		}
	}

	public void printResults() {
		for (int index = 0, size = this.results.size(); index < size; index++) {
			System.out.println("Level " + index + ':');
			this.results.get(index).stream().map(UnregisteredObjectException::getID).forEach(System.out::println);
		}
	}
}