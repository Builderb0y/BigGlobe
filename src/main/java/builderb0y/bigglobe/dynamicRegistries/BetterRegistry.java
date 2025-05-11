package builderb0y.bigglobe.dynamicRegistries;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	public abstract @Nullable RegistryEntry<T> getEntry(RegistryKey<T> key);

	public default @NotNull RegistryEntry<T> requireEntry(RegistryKey<T> key) {
		RegistryEntry<T> entry = this.getEntry(key);
		if (entry != null) return entry;
		StringBuilder message = new StringBuilder(128).append("Key ").append(key.getValue()).append(" not present in registry ").append(this.getKey().getValue()).append('.');
		if (this.getTag(TagKey.of(RegistryVersions.getRegistryKey(key), key.getValue()), false) != null) {
			message.append(" Note: a tag with this name exists. Did you forget to prefix the name with '#'?");
		}
		throw new IllegalStateException(message.toString());
	}

	public abstract @Nullable RegistryEntryList<T> getTag(TagKey<T> key, boolean create);

	public default @NotNull RegistryEntryList<T> requireTag(TagKey<T> key) {
		RegistryEntryList<T> tag = this.getTag(key, true);
		if (tag != null) return tag;
		StringBuilder message = new StringBuilder().append("Tag ").append(key.id()).append(" not present in registry ").append(this.getKey().getValue()).append('.');
		if (this.getEntry(RegistryKey.of(RegistryVersions.getRegistryKey(key), key.id())) != null) {
			message.append(" Note: an entry with this name exists. Did you prefix the name with '#' by mistake?");
		}
		throw new NullPointerException(message.toString());
	}

	public abstract Stream<RegistryEntry<T>> streamEntries();

	public abstract Stream<RegistryEntryList<T>> streamTags();

	public default RegistryEntry<T> requireById(Identifier id) {
		return this.requireEntry(RegistryKey.of(this.getKey(), id));
	}

	public default RegistryEntry<T> requireByName(String name) {
		return this.requireById(IdentifierVersions.create(name));
	}

	public default RegistryEntry<T> getById(Identifier id) {
		return this.getEntry(RegistryKey.of(this.getKey(), id));
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
		public RegistryEntry<T> getEntry(RegistryKey<T> key) {
			#if MC_VERSION >= MC_1_21_2
				return this.registry.getOptional(key).orElse(null);
			#else
				return this.registry.getEntry(key).orElse(null);
			#endif
		}

		@Override
		public RegistryEntryList<T> getTag(TagKey<T> key, boolean create) {
			#if MC_VERSION >= MC_1_21_2
				return this.registry.getOptional(key).orElse(null);
			#else
				return this.registry.getEntryList(key).orElse(null);
			#endif
		}

		@Override
		public Stream<RegistryEntry<T>> streamEntries() {
			return castStream(this.registry.streamEntries());
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Stream<RegistryEntryList<T>> streamTags() {
			#if MC_VERSION >= MC_1_21_2
				return castStream(this.registry.streamTags());
			#else
				return this.registry.streamTagsAndEntries().map(Pair::getSecond);
			#endif
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
		public RegistryEntry<T> getEntry(RegistryKey<T> key) {
			return this.lookup.getOptional(key).orElse(null);
		}

		@Override
		public RegistryEntryList<T> getTag(TagKey<T> key, boolean create) {
			return (create ? this.lookup : this.wrapperImpl).getOptional(key).orElse(null);
		}

		@Override
		public Stream<RegistryEntry<T>> streamEntries() {
			return castStream(this.wrapperImpl.streamEntries());
		}

		@Override
		public Stream<RegistryEntryList<T>> streamTags() {
			#if MC_VERSION >= MC_1_21_2
				return castStream(this.wrapperImpl.getTags());
			#else
				return castStream(this.wrapperImpl.streamTags());
			#endif
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