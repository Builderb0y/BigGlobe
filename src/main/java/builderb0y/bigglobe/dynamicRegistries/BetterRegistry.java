package builderb0y.bigglobe.dynamicRegistries;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

/**
in 1.19.2, all of this functionality was implemented by {@link Registry}.
but in 1.19.4 and later, this functionality is split between {@link RegistryEntryLookup}
and {@link RegistryWrapper.Impl}. and worse yet, this is only the case for dynamic registries.
hard-coded registries still work with just {@link Registry}.
so, I have this interface to act as a compatibility layer, allowing me to tweak it
in different versions as-needed without changing how it's presented to other classes.
*/
public interface BetterRegistry<T> {

	public abstract RegistryKey<Registry<T>> getKey();

	public abstract RegistryEntry<T> getOrCreateEntry(RegistryKey<T> key);

	public abstract RegistryEntryList<T> getOrCreateTag(TagKey<T> key);

	public abstract Stream<RegistryEntry<T>> streamEntries();

	public abstract Stream<RegistryEntryList<T>> streamTags();

	public default RegistryEntry<T> getById(Identifier id) {
		return this.getOrCreateEntry(RegistryKey.of(this.getKey(), id));
	}

	public default RegistryEntry<T> getByName(String name) {
		return this.getById(IdentifierVersions.create(name));
	}

	public default Iterable<RegistryEntry<T>> entries() {
		return this.streamEntries()::iterator;
	}

	public default Iterable<RegistryKey<T>> keys() {
		return this.streamEntries().map(UnregisteredObjectException::getKey)::iterator;
	}

	public default Iterable<T> values() {
		return this.streamEntries().map(RegistryEntry::value)::iterator;
	}

	public static class BetterHardCodedRegistry<T> implements BetterRegistry<T> {

		public final Registry<T> registry;

		public BetterHardCodedRegistry(Registry<T> registry) {
			this.registry = registry;
		}

		@Override
		public RegistryKey<Registry<T>> getKey() {
			return RegistryVersions.getRegistryKey(this.registry);
		}

		@Override
		public RegistryEntry<T> getOrCreateEntry(RegistryKey<T> key) {
			return this.registry.entryOf(key);
		}

		@Override
		public RegistryEntryList<T> getOrCreateTag(TagKey<T> key) {
			return this.registry.getOrCreateEntryList(key);
		}

		@Override
		public Stream<RegistryEntry<T>> streamEntries() {
			return castStream(this.registry.streamEntries());
		}

		@Override
		public Stream<RegistryEntryList<T>> streamTags() {
			return this.registry.streamTagsAndEntries().map(Pair::getSecond);
		}
	}

	public static class BetterDynamicRegistry<T> implements BetterRegistry<T> {

		public final RegistryWrapper.Impl<T> wrapperImpl;
		public final RegistryEntryLookup<T> lookup;

		public BetterDynamicRegistry(RegistryWrapper.Impl<T> wrapperImpl, RegistryEntryLookup<T> lookup) {
			this.wrapperImpl = wrapperImpl;
			this.lookup = lookup;
		}

		@Override
		public RegistryKey<Registry<T>> getKey() {
			return RegistryVersions.getRegistryKey(this.wrapperImpl);
		}

		@Override
		public RegistryEntry<T> getOrCreateEntry(RegistryKey<T> key) {
			return this.lookup.getOrThrow(key);
		}

		@Override
		public RegistryEntryList<T> getOrCreateTag(TagKey<T> key) {
			return this.lookup.getOrThrow(key);
		}

		@Override
		public Stream<RegistryEntry<T>> streamEntries() {
			return castStream(this.wrapperImpl.streamEntries());
		}

		@Override
		public Stream<RegistryEntryList<T>> streamTags() {
			return castStream(this.wrapperImpl.streamTags());
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> castStream(Stream<? extends T> stream) {
		return (Stream<T>)(stream);
	}

	public static interface Lookup {

		public abstract <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key);
	}
}