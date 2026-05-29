package builderb0y.bigglobe.util;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.SimpleDelayedCompileable;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.noise.Permuter;

public class DelayedEntryList<T> implements SimpleDelayedCompileable {

	public static final Comparator<Holder<?>> COMPARATOR = Comparator.comparing(UnregisteredObjectException::getID);

	public final @Nullable BetterRegistry<T> resolver;
	public final @NotNull ResourceKey<Registry<T>> registryKey;
	public final @NotNull
	@SingletonArray List<DelayedEntry> delayedEntries;
	public SortedEncodings sortedEncodings;
	public boolean compileCalled = true;

	public @Nullable List<Holder<T>> entryList;
	public @Nullable Set<Holder<T>> entrySet;
	public @Nullable List<T> objectList;
	public @Nullable Set<T> objectSet;
	public @Nullable HolderSet<T> tag;

	public DelayedEntryList(ResourceKey<Registry<T>> key) {
		this.resolver = null;
		this.registryKey = key;
		this.delayedEntries = Collections.emptyList();
		this.sortedEncodings = new SortedEncodings(Collections.emptyList());
		this.entryList = Collections.emptyList();
		this.entrySet = Collections.emptySet();
		this.objectList = Collections.emptyList();
		this.objectSet = Collections.emptySet();

		this.tag = HolderSet.empty();
	}

	public DelayedEntryList(
		@NotNull BetterRegistry<T> resolver,
		@NotNull @SingletonArray List<DelayedEntry> delayedEntries
	) {
		this.resolver = resolver;
		this.registryKey = resolver.getKey();
		this.delayedEntries = delayedEntries;
	}

	public DelayedEntryList(@NotNull BetterRegistry<T> resolver, @NotNull HolderSet<T> list) {
		this.resolver = resolver;
		this.registryKey = resolver.getKey();
		Optional<TagKey<T>> key = list.unwrapKey();
		if (key.isPresent()) {
			this.delayedEntries = Collections.singletonList(
				new DelayedEntry('#' + key.get().location().toString())
			);
		}
		else {
			this.delayedEntries = list.stream().map(UnregisteredObjectException::getID).map((Identifier id) -> new DelayedEntry(id, false)).toList();
			this.entryList = list.stream().toList();
		}
	}

	public static <T> DelayedEntryList<T> empty(ResourceKey<Registry<T>> key) {
		return new DelayedEntryList<>(key);
	}

	public static <T> DelayedEntryList<T> emptyOnClient(ResourceKey<Registry<T>> key, boolean client, String... ids) {
		return client ? empty(key) : create(key, false, ids);
	}

	public static <T> DelayedEntryList<T> create(ResourceKey<Registry<T>> key, boolean client, String... args) {
		List<DelayedEntry> list = Arrays.stream(args).filter(Objects::nonNull).map(DelayedEntry::new).toList();
		return list.isEmpty() ? empty(key) : new DelayedEntryList<>(BigGlobeMod.getSidedRegistry(key, client), list);
	}

	public static <T> DelayedEntryList<T> create(ResourceKey<Registry<T>> registryKey, boolean client, String input) {
		if (input == null) return empty(registryKey);
		return new DelayedEntryList<>(BigGlobeMod.getSidedRegistry(registryKey, client), Collections.singletonList(new DelayedEntry(input)));
	}

	public static <T> DelayedEntryList<T> create(BetterRegistry<T> registry, String input) {
		if (input == null) return empty(registry.getKey());
		return new DelayedEntryList<>(registry, Collections.singletonList(new DelayedEntry(input)));
	}

	public boolean isResolved() {
		return this.entryList != null;
	}

	public List<Holder<T>> entryList() {
		List<Holder<T>> entryList = this.entryList;
		if (entryList == null) {
			entryList = this.entryList = this.resolve();
		}
		return entryList;
	}

	public Set<Holder<T>> entrySet() {
		Set<Holder<T>> entrySet = this.entrySet;
		if (entrySet == null) {
			entrySet = this.entrySet = Set.copyOf(this.entryList());
		}
		return entrySet;
	}

	public List<T> objectList() {
		List<T> objectList = this.objectList;
		if (objectList == null) {
			objectList = this.objectList = this.entryList().stream().map(Holder<T>::value).toList();
		}
		return objectList;
	}

	public Set<T> objectSet() {
		Set<T> objectSet = this.objectSet;
		if (objectSet == null) {
			objectSet = this.objectSet = this.entryList().stream().map(Holder<T>::value).collect(Collectors.toUnmodifiableSet());
		}
		return objectSet;
	}

	public HolderSet<T> tag() {
		if (this.tag == null) {
			this.tag = HolderSet.direct(this.entryList());
		}
		return this.tag;
	}

	public Stream<Holder<T>> entryStream() {
		return this.entryList().stream();
	}

	public Stream<T> objectStream() {
		return this.entryList().stream().map(Holder<T>::value);
	}

	public boolean contains(T object) {
		return this.objectSet().contains(object);
	}

	public boolean contains(Holder<T> entry) {
		return this.entrySet().contains(entry);
	}

	public int size() {
		return this.entryList().size();
	}

	public boolean isEmpty() {
		return this.entryList().isEmpty();
	}

	public T randomObject(RandomGenerator random) {
		return Permuter.choose(random, this.entryList()).value();
	}

	public Holder<T> randomEntry(RandomGenerator random) {
		return Permuter.choose(random, this.entryList());
	}

	public T randomObject(long seed) {
		return Permuter.choose(seed, this.entryList()).value();
	}

	public Holder<T> randomEntry(long seed) {
		return Permuter.choose(seed, this.entryList());
	}

	public List<Holder<T>> resolve() {
		if (this.resolver == null) {
			throw new IllegalStateException("Can't resolve DelayedEntryList with no registry!");
		}
		if (!this.compileCalled) {
			BigGlobeMod.LOGGER.warn("Something is trying to resolve a DelayedEntryList too early!", new IllegalStateException("Stack trace"));
		}
		List<Holder<T>> entryList = this.entryList = (
			this
			.delayedEntries
			.stream()
			.flatMap((DelayedEntry element) -> {
				if (element.isTag()) {
					TagKey<T> key = TagKey.create(this.registryKey, element.id);
					HolderSet<T> resolution = this.resolver.requireTag(key);
					if (resolution.size() == 0 && BigGlobeConfig.INSTANCE.get().dataPackDebugging.emptyTags) {
						BigGlobeMod.LOGGER.warn("Empty tag: " + key);
					}
					return resolution.stream();
				}
				else {
					return Stream.of(this.resolver.requireById(element.id));
				}
			})
			.collect(
				Collector.of(
					() -> new TreeSet<>(COMPARATOR),
					TreeSet::add,
					(TreeSet<Holder<T>> set1, TreeSet<Holder<T>> set2) -> {
						set1.addAll(set2);
						return set1;
					},
					List::copyOf
				)
			)
		);
		this.objectList = null;
		this.objectSet = null;
		this.entrySet = null;
		return entryList;
	}

	@Override
	public void compile() {
		if (!this.compileCalled) {
			this.compileCalled = true;
			this.resolve();
		}
	}

	@Override
	public void delay() {
		ColumnEntryRegistry.Loading.get().addTag(this);
	}

	public SortedEncodings getSortedEncodings() {
		if (this.sortedEncodings == null) {
			this.sortedEncodings = new SortedEncodings(this.delayedEntries);
		}
		return this.sortedEncodings;
	}

	/**
	equality semantics:
	two DelayedEntryList's are considered equal if they have the
	same {@link #resolver}'s {@link BetterRegistry#getKey()}
	and the same {@link #delayedEntries}.
	however, the order of entries is irrelevant, since
	all orders will produce the same {@link #entryList}.

	equality implementation:
	to make the equality check as fast as possible, we could do a few different things:
	1. we could convert the list to a set so that a contains() check is faster.
	2. we could intern all the delayed entries' encodings, so that equality checking is faster.
	3. we could pre-sort the encodings so that we don't need to compute the hash code of them.
	if two arrays are both sorted, then we can compare them element-by-element.
	4. we could pre-compute the hash code of the sorted array as a fast path.
	5. we could intern the array and check reference equality on *that*.

	I opted to do 2, 3, and 4, but not 5.
	*/
	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof DelayedEntryList<?> that &&
			((ResourceKey<?>)(this.registryKey)) == ((ResourceKey<?>)(that.registryKey)) &&
			this.getSortedEncodings().equals(that.getSortedEncodings())
		);
	}

	@Override
	public int hashCode() {
		return this.getSortedEncodings().hashCode();
	}

	@Override
	public String toString() {
		List<DelayedEntry> entries = this.delayedEntries;
		int size = entries.size();
		StringBuilder builder = new StringBuilder((size + 1) << 6).append(this.registryKey.identifier());
		if (size == 0) return builder.append("[]").toString();
		builder.append("[ ").append(entries.get(0).encoding);
		for (int index = 1; index < size; index++) {
			builder.append(", ").append(entries.get(index));
		}
		return builder.append(" ]").toString();
	}

	public static class SortedEncodings {

		public final String[] array;
		public final int hashCode;

		public SortedEncodings(List<DelayedEntry> entries) {
			this.array = entries.stream().map(DelayedEntry::encoding).sorted().toArray(String[]::new);
			this.hashCode = Arrays.hashCode(this.array);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DelayedEntryList.SortedEncodings that && this.hashCode == that.hashCode) {
				String[] a = this.array, b = that.array;
				int length = a.length;
				if (length == b.length) {
					for (int index = 0; index < length; index++) {
						if (a[index] != b[index]) return false;
					}
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner(", ", "[ ", " ]").setEmptyValue("[]");
			for (String string : this.array) {
				joiner.add(string);
			}
			return joiner.toString();
		}
	}
}