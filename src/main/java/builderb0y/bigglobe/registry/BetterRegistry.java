package builderb0y.bigglobe.registry;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.util.UnregisteredObjectException;

public class BetterRegistry<T> {

	public final RegistryKey<Registry<T>> registryKey;
	public final Map<Identifier, BetterRegistryEntry<T>> keyToEntry;
	public final Map<T, BetterRegistryEntry<T>> objectToEntry;
	public final Map<Identifier, BetterTag<T>> tags;

	public BetterRegistry(RegistryKey<Registry<T>> registryKey, Stream<? extends RegistryEntry<T>> elements, Stream<? extends RegistryEntryList<T>> tags) {
		this.registryKey = registryKey;
		this.keyToEntry = elements.collect(
			Collectors.toMap(
				(RegistryEntry<T> entry) -> entry.getKey().orElseThrow().getValue(),
				(RegistryEntry<T> entry) -> new BetterRegistryEntry<>(
					entry,
					UnregisteredObjectException.getKey(entry),
					entry.value()
				)
			)
		);
		this.objectToEntry = this.keyToEntry.values().stream().collect(
			Collectors.toMap(
				BetterRegistryEntry::object,
				Function.identity()
			)
		);
		this.tags = (
			tags
			.map((RegistryEntryList<T> list) -> new BetterTag<>(
				UnregisteredObjectException.getTagKey(list),
				list,
				list.stream().map(
					(RegistryEntry<T> entry) -> this.getEntry(
						UnregisteredObjectException.getKey(entry)
					)
				)
				.collect(Collectors.toList())
			))
			.collect(Collectors.toMap(
				(BetterTag<T> tag) -> tag.key().id(),
				Function.identity()
			))
		);
	}

	public BetterRegistry(RegistryKey<Registry<T>> registryKey, RegistryWrapper<T> wrapper) {
		this(registryKey, wrapper.streamEntries(), wrapper.streamTags());
	}

	@SuppressWarnings("unchecked")
	public BetterRegistry(RegistryWrapper.Impl<T> wrapper) {
		this((RegistryKey<Registry<T>>)(wrapper.getRegistryKey()), wrapper);
	}

	@SuppressWarnings("unchecked")
	public BetterRegistry(Registry<T> registry) {
		this((RegistryKey<Registry<T>>)(registry.getKey()), registry.streamEntries(), registry.streamTagsAndEntries().map(Pair::getSecond));
	}

	public BetterRegistryEntry<T> getEntry(RegistryKey<T> key) {
		return this.getEntry(key.getValue());
	}

	public BetterRegistryEntry<T> getEntry(Identifier identifier) {
		BetterRegistryEntry<T> entry = this.keyToEntry.get(identifier);
		if (entry != null) return entry;
		else throw new UnregisteredObjectException(String.valueOf(identifier));
	}

	public T getObject(Identifier identifier) {
		return this.getEntry(identifier).object();
	}

	public T getObject(RegistryKey<T> key) {
		return this.getEntry(key).object();
	}

	public BetterRegistryEntry<T> entryOf(T object) {
		BetterRegistryEntry<T> entry = this.objectToEntry.get(object);
		if (entry != null) return entry;
		else throw new UnregisteredObjectException(String.valueOf(object));
	}

	public RegistryKey<T> getKey(T object) {
		return this.entryOf(object).key();
	}

	public Identifier getID(T object) {
		return this.entryOf(object).id();
	}

	public BetterTag<T> getTag(Identifier identifier) {
		BetterTag<T> tag = this.tags.get(identifier);
		if (tag != null) return tag;
		else throw new UnregisteredObjectException("Missing tag: " + identifier);
	}

	public BetterTag<T> getTag(TagKey<T> key) {
		return this.getTag(key.id());
	}

	@Override
	public int hashCode() {
		return this.registryKey.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof BetterRegistry<?> that && this.registryKey.equals(that.registryKey));
	}

	@Override
	public String toString() {
		return "BetterRegistry: " + this.registryKey + " (" + this.keyToEntry.size() + " elements)";
	}
}