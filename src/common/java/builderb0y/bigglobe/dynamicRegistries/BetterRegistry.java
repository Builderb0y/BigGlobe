package builderb0y.bigglobe.dynamicRegistries;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

/**
in 1.19.2, all of this functionality was implemented by {@link Registry}.
but in 1.19.4 and later, this functionality is split between {@link HolderGetter}
and {@link HolderLookup.RegistryLookup}. and worse yet, this is only the case for dynamic registries.
hard-coded registries still work with just {@link Registry}.
so, I have this interface to act as a compatibility layer, allowing me to tweak it
in different versions as-needed without changing how it's presented to other classes.
*/
public interface BetterRegistry<T> {

	public abstract ResourceKey<Registry<T>> getKey();

	public abstract @Nullable Holder<T> getEntry(ResourceKey<T> key);

	public default @NotNull Holder<T> requireEntry(ResourceKey<T> key) {
		Holder<T> entry = this.getEntry(key);
		if (entry != null) return entry;
		StringBuilder message = new StringBuilder(128).append("Key ").append(key.identifier()).append(" not present in registry ").append(this.getKey().identifier()).append('.');
		if (this.getTag(TagKey.create(RegistryVersions.getRegistryKey(key), key.identifier()), false) != null) {
			message.append(" Note: a tag with this name exists. Did you forget to prefix the name with '#'?");
		}
		throw new IllegalStateException(message.toString());
	}

	public abstract @Nullable HolderSet<T> getTag(TagKey<T> key, boolean create);

	public default @NotNull HolderSet<T> requireTag(TagKey<T> key) {
		HolderSet<T> tag = this.getTag(key, true);
		if (tag != null) return tag;
		StringBuilder message = new StringBuilder().append("Tag ").append(key.location()).append(" not present in registry ").append(this.getKey().identifier()).append('.');
		if (this.getEntry(ResourceKey.create(RegistryVersions.getRegistryKey(key), key.location())) != null) {
			message.append(" Note: an entry with this name exists. Did you prefix the name with '#' by mistake?");
		}
		throw new NullPointerException(message.toString());
	}

	public abstract Stream<Holder<T>> streamEntries();

	public abstract Stream<HolderSet<T>> streamTags();

	public default Holder<T> requireById(Identifier id) {
		return this.requireEntry(ResourceKey.create(this.getKey(), id));
	}

	public default Holder<T> requireByName(String name) {
		return this.requireById(IdentifierVersions.create(name));
	}

	public default Holder<T> getById(Identifier id) {
		return this.getEntry(ResourceKey.create(this.getKey(), id));
	}

	public default Holder<T> getByName(String name) {
		return this.getById(IdentifierVersions.create(name));
	}

	public default Iterable<Holder<T>> entries() {
		return this.streamEntries()::iterator;
	}

	public default Iterable<ResourceKey<T>> keys() {
		return this.streamEntries().map(UnregisteredObjectException::getKey)::iterator;
	}

	public default Iterable<T> values() {
		return this.streamEntries().map(Holder::value)::iterator;
	}

	public static class BetterHardCodedRegistry<T> implements BetterRegistry<T> {

		public final Registry<T> registry;

		public BetterHardCodedRegistry(Registry<T> registry) {
			this.registry = registry;
		}

		@Override
		public ResourceKey<Registry<T>> getKey() {
			return RegistryVersions.getRegistryKey(this.registry);
		}

		@Override
		public Holder<T> getEntry(ResourceKey<T> key) {

			return this.registry.get(key).orElse(null);
		}

		@Override
		public HolderSet<T> getTag(TagKey<T> key, boolean create) {

			return this.registry.get(key).orElse(null);
		}

		@Override
		public Stream<Holder<T>> streamEntries() {
			return castStream(this.registry.listElements());
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Stream<HolderSet<T>> streamTags() {

			return castStream(this.registry.getTags());
		}
	}

	public static class BetterDynamicRegistry<T> implements BetterRegistry<T> {

		public final HolderLookup.RegistryLookup<T> wrapperImpl;
		public final HolderGetter<T> lookup;

		public BetterDynamicRegistry(HolderLookup.RegistryLookup<T> wrapperImpl, HolderGetter<T> lookup) {
			this.wrapperImpl = wrapperImpl;
			this.lookup = lookup;
		}

		@Override
		public ResourceKey<Registry<T>> getKey() {
			return RegistryVersions.getRegistryKey(this.wrapperImpl);
		}

		@Override
		public Holder<T> getEntry(ResourceKey<T> key) {
			return this.lookup.get(key).orElse(null);
		}

		@Override
		public HolderSet<T> getTag(TagKey<T> key, boolean create) {
			return (create ? this.lookup : this.wrapperImpl).get(key).orElse(null);
		}

		@Override
		public Stream<Holder<T>> streamEntries() {
			return castStream(this.wrapperImpl.listElements());
		}

		@Override
		public Stream<HolderSet<T>> streamTags() {

			return castStream(this.wrapperImpl.listTags());
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> castStream(Stream<? extends T> stream) {
		return (Stream<T>)(stream);
	}

	public static interface Lookup {

		public abstract <T> BetterRegistry<T> getRegistry(ResourceKey<Registry<T>> key);
	}
}