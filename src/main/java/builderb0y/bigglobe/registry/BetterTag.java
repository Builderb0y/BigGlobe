package builderb0y.bigglobe.registry;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

public record BetterTag<T>(
	TagKey<T> key,
	RegistryEntryList<T> vanilla,
	List<BetterRegistryEntry<T>> elementsList,
	Set<BetterRegistryEntry<T>> elementsSet
)
implements Collection<BetterRegistryEntry<T>> {

	public BetterTag {
		if (elementsList.size() != elementsSet.size() || !elementsSet.containsAll(elementsList)) {
			throw new IllegalArgumentException("List and Set views contain different elements!");
		}
	}

	public BetterTag(TagKey<T> key, RegistryEntryList<T> vanilla, List<BetterRegistryEntry<T>> elementsList) {
		this(key, vanilla, elementsList, new HashSet<>(elementsList));
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof BetterTag<?> that && this.key.equals(that.key));
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public String toString() {
		return "BetterTag " + this.key + ": " + this.elementsList;
	}

	@Override
	public int size() {
		return this.elementsList.size();
	}

	@Override
	public boolean isEmpty() {
		return this.elementsList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.elementsSet.contains(o);
	}

	@NotNull
	@Override
	public Iterator<BetterRegistryEntry<T>> iterator() {
		return this.elementsList.iterator();
	}

	@NotNull
	@Override
	public Object[] toArray() {
		return this.elementsList.toArray();
	}

	@NotNull
	@Override
	public <T> T[] toArray(@NotNull T[] a) {
		return this.elementsList.toArray(a);
	}

	@Override
	public boolean add(BetterRegistryEntry<T> entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> c) {
		return this.elementsSet.containsAll(c);
	}

	@Override
	public boolean addAll(@NotNull Collection<? extends BetterRegistryEntry<T>> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(@NotNull Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(IntFunction<T[]> generator) {
		return this.toArray(generator.apply(this.size()));
	}

	@Override
	public boolean removeIf(Predicate<? super BetterRegistryEntry<T>> filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Spliterator<BetterRegistryEntry<T>> spliterator() {
		return this.elementsList.spliterator();
	}

	@Override
	public Stream<BetterRegistryEntry<T>> stream() {
		return this.elementsList.stream();
	}

	@Override
	public Stream<BetterRegistryEntry<T>> parallelStream() {
		return this.elementsList.parallelStream();
	}

	@Override
	public void forEach(Consumer<? super BetterRegistryEntry<T>> action) {
		this.elementsList.forEach(action);
	}
}