package builderb0y.bigglobe.util;

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.DelayedCompileable;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.parsing.ScriptParsingException;

public class DelayedEntryList<T> implements DelayedCompileable {

	public static final Comparator<RegistryEntry<?>> COMPARATOR = Comparator.comparing(UnregisteredObjectException::getID);

	public final @Nullable BetterRegistry<T> resolver;
	public final @NotNull RegistryKey<Registry<T>> registryKey;
	public final @NotNull @SingletonArray List<DelayedEntry> delayedEntries;
	public SortedEncodings sortedEncodings;
	public boolean compileCalled = true;

	public @Nullable List<RegistryEntry<T>> entryList;
	public @Nullable Set<RegistryEntry<T>> entrySet;
	public @Nullable List<T> objectList;
	public @Nullable Set<T> objectSet;
	public @Nullable RegistryEntryList<T> tag;

	public DelayedEntryList(RegistryKey<Registry<T>> key) {
		this.resolver = null;
		this.registryKey = key;
		this.delayedEntries = Collections.emptyList();
		this.sortedEncodings = new SortedEncodings(Collections.emptyList());
		this.entryList = Collections.emptyList();
		this.entrySet = Collections.emptySet();
		this.objectList = Collections.emptyList();
		this.objectSet = Collections.emptySet();
		#if MC_VERSION >= MC_1_20_5
			this.tag = RegistryEntryList.empty();
		#else
			this.tag = RegistryEntryList.of();
		#endif
	}

	public DelayedEntryList(
		@NotNull BetterRegistry<T> resolver,
		@NotNull @SingletonArray List<DelayedEntry> delayedEntries
	) {
		this.resolver = resolver;
		this.registryKey = resolver.getKey();
		this.delayedEntries = delayedEntries;
	}

	public DelayedEntryList(@NotNull BetterRegistry<T> resolver, @NotNull RegistryEntryList<T> list) {
		this.resolver = resolver;
		this.registryKey = resolver.getKey();
		Optional<TagKey<T>> key = list.getTagKey();
		if (key.isPresent()) {
			this.delayedEntries = Collections.singletonList(
				new DelayedEntry('#' + key.get().id().toString())
			);
		}
		else {
			this.delayedEntries = list.stream().map(UnregisteredObjectException::getID).map((Identifier id) -> new DelayedEntry(id, false)).toList();
			this.entryList = list.stream().toList();
		}
	}

	public static <T> DelayedEntryList<T> empty(RegistryKey<Registry<T>> key) {
		return new DelayedEntryList<>(key);
	}

	public static <T> DelayedEntryList<T> emptyOnClient(RegistryKey<Registry<T>> key, boolean client, String... ids) {
		return client ? empty(key) : create(key, false, ids);
	}

	public static <T> DelayedEntryList<T> create(RegistryKey<Registry<T>> key, boolean client, String... args) {
		List<DelayedEntry> list = Arrays.stream(args).filter(Objects::nonNull).map(DelayedEntry::new).toList();
		return list.isEmpty() ? empty(key) : new DelayedEntryList<>(BigGlobeMod.getSidedRegistry(key, client), list);
	}

	public static <T> DelayedEntryList<T> create(RegistryKey<Registry<T>> registryKey, boolean client, String input) {
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

	public List<RegistryEntry<T>> entryList() {
		if (!this.isResolved()) {
			this.resolve();
		}
		return this.entryList;
	}

	public Set<RegistryEntry<T>> entrySet() {
		if (this.entrySet == null) {
			this.entrySet = Set.copyOf(this.entryList());
		}
		return this.entrySet;
	}

	public List<T> objectList() {
		if (this.objectList == null) {
			this.objectList = this.entryList().stream().map(RegistryEntry<T>::value).toList();
		}
		return this.objectList;
	}

	public Set<T> objectSet() {
		if (this.objectSet == null) {
			this.objectSet = this.entryList().stream().map(RegistryEntry<T>::value).collect(Collectors.toUnmodifiableSet());
		}
		return this.objectSet;
	}

	public RegistryEntryList<T> tag() {
		if (this.tag == null) {
			this.tag = RegistryEntryList.of(this.entryList());
		}
		return this.tag;
	}

	public Stream<RegistryEntry<T>> entryStream() {
		return this.entryList().stream();
	}

	public Stream<T> objectStream() {
		return this.entryList().stream().map(RegistryEntry<T>::value);
	}

	public boolean contains(T object) {
		return this.objectSet().contains(object);
	}

	public boolean contains(RegistryEntry<T> entry) {
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

	public RegistryEntry<T> randomEntry(RandomGenerator random) {
		return Permuter.choose(random, this.entryList());
	}

	public T randomObject(long seed) {
		return Permuter.choose(seed, this.entryList()).value();
	}

	public RegistryEntry<T> randomEntry(long seed) {
		return Permuter.choose(seed, this.entryList());
	}

	public void resolve() {
		if (this.resolver == null) {
			throw new IllegalStateException("Can't resolve DelayedEntryList with no registry!");
		}
		if (!this.compileCalled) {
			BigGlobeMod.LOGGER.warn("Something is trying to resolve a DelayedEntryList too early!", new IllegalStateException("Stack trace"));
		}
		this.entryList = (
			this
			.delayedEntries
			.stream()
			.flatMap((DelayedEntry element) -> {
				if (element.isTag()) {
					TagKey<T> key = TagKey.of(this.registryKey, element.id);
					RegistryEntryList<T> resolution = this.resolver.requireTag(key);
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
					() -> new TreeSet<RegistryEntry<T>>(COMPARATOR),
					TreeSet::add,
					(TreeSet<RegistryEntry<T>> set1, TreeSet<RegistryEntry<T>> set2) -> {
						set1.addAll(set2);
						return set1;
					},
					List::copyOf
				)
			)
		);
		this.entrySet = null;
		this.objectList = null;
		this.objectSet = null;
	}

	@Override
	public void compile(ColumnEntryRegistry registry) {
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
			((RegistryKey<?>)(this.registryKey)) == ((RegistryKey<?>)(that.registryKey)) &&
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
		StringBuilder builder = new StringBuilder((size + 1) << 6).append(this.registryKey.getValue());
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