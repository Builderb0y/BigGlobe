package builderb0y.bigglobe.dynamicRegistries;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.*;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import builderb0y.bigglobe.util.Grouper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

/** version-independent abstraction of the functionality I actually need from registries. */
public interface RegistryAdapter<T> {

	public abstract ResourceKey<Registry<T>> getKey();

	public abstract @Nullable Holder<T> getEntry(ResourceKey<T> key, boolean create);

	public default @Nullable Holder<T> getEntry(ResourceKey<T> key) {
		return this.getEntry(key, false);
	}

	public default @NotNull Holder<T> requireEntry(ResourceKey<T> key) {
		Holder<T> entry = this.getEntry(key, true);
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
		if (this.getEntry(ResourceKey.create(RegistryVersions.getRegistryKey(key), key.location()), false) != null) {
			message.append(" Note: an entry with this name exists. Did you prefix the name with '#' by mistake?");
		}
		throw new NullPointerException(message.toString());
	}

	public default Holder<T> requireById(Identifier id) {
		return this.requireEntry(ResourceKey.create(this.getKey(), id));
	}

	public default Holder<T> requireByName(String name) {
		return this.requireById(IdentifierVersions.create(name));
	}

	public default Holder<T> getById(Identifier id) {
		return this.getEntry(ResourceKey.create(this.getKey(), id), false);
	}

	public default Holder<T> getByName(String name) {
		return this.getById(IdentifierVersions.create(name));
	}

	public abstract HolderGetter<T> toVanilla();

	public static interface StreamableRegistryAdapter<T> extends RegistryAdapter<T> {

		public abstract Stream<Holder<T>> streamEntries();

		public abstract Stream<HolderSet<T>> streamTags();

		public default Iterable<Holder<T>> entries() {
			return this.streamEntries()::iterator;
		}

		public default Iterable<ResourceKey<T>> keys() {
			return this.streamEntries().map(UnregisteredObjectException::getKey)::iterator;
		}

		public default Iterable<T> values() {
			return this.streamEntries().map(Holder::value)::iterator;
		}
	}

	public static class BasicImpl<T> implements RegistryAdapter<T> {

		public final ResourceKey<Registry<T>> key;
		public final HolderGetter<T> delegate;

		public BasicImpl(ResourceKey<Registry<T>> key, HolderGetter<T> delegate) {
			this.key = key;
			this.delegate = delegate;
		}

		@Override
		public ResourceKey<Registry<T>> getKey() {
			return this.key;
		}

		@Override
		public @Nullable Holder<T> getEntry(ResourceKey<T> key, boolean create) {
			return create ? this.delegate.getOrThrow(key) : this.delegate.get(key).orElse(null);
		}

		@Override
		public @Nullable HolderSet<T> getTag(TagKey<T> key, boolean create) {
			return create ? this.delegate.getOrThrow(key) : this.delegate.get(key).orElse(null);
		}

		@Override
		public HolderGetter<T> toVanilla() {
			return this.delegate;
		}
	}

	public static class StreamableImpl<T> implements StreamableRegistryAdapter<T> {

		public final RegistryLookup<T> delegate;

		public StreamableImpl(RegistryLookup<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		@SuppressWarnings("unchecked")
		public ResourceKey<Registry<T>> getKey() {
			return (ResourceKey<Registry<T>>)(this.delegate.key());
		}

		@Override
		public @Nullable Holder<T> getEntry(ResourceKey<T> key, boolean create) {
			return create ? this.delegate.getOrThrow(key) : this.delegate.get(key).orElse(null);
		}

		@Override
		public @Nullable HolderSet<T> getTag(TagKey<T> key, boolean create) {
			return create ? this.delegate.getOrThrow(key) : this.delegate.get(key).orElse(null);
		}

		@Override
		public Stream<Holder<T>> streamEntries() {
			return Grouper.castStream(this.delegate.listElements());
		}

		@Override
		public Stream<HolderSet<T>> streamTags() {
			return Grouper.castStream(this.delegate.listTags());
		}

		@Override
		public HolderGetter<T> toVanilla() {
			return this.delegate;
		}
	}

	public static interface Lookup {

		public abstract <T> RegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key);

		public static Lookup from(HolderGetter.Provider provider) {
			return new Lookup() {

				@Override
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public <T> RegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key) {
					HolderGetter<T> getter = provider.lookup(key).orElse(null);
					if (getter == null) {
						getter = (HolderGetter<T>)(BuiltInRegistries.REGISTRY.get((ResourceKey)(key)).orElse(null));
						if (getter == null) {
							throw new NoSuchRegistryException(key.identifier().toString());
						}
					}
					return new BasicImpl<>(key, getter);
				}
			};
		}
	}

	public static interface StreamableLookup extends Lookup {

		@Override
		public abstract <T> StreamableRegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key);

		public static StreamableLookup from(RegistryLookup.Provider provider) {
			return new StreamableLookup() {

				@Override
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public <T> StreamableRegistryAdapter<T> getRegistry(ResourceKey<Registry<T>> key) {
					RegistryLookup<T> lookup = provider.lookup(key).orElse(null);
					if (lookup == null) {
						lookup = (RegistryLookup<T>)(BuiltInRegistries.REGISTRY.get((ResourceKey)(key)).orElse(null));
						if (lookup == null) {
							throw new NoSuchRegistryException(key.identifier().toString());
						}
					}
					return new StreamableImpl<>(lookup);
				}
			};
		}
	}

	public static class NoSuchRegistryException extends RuntimeException {

		public NoSuchRegistryException() {}

		public NoSuchRegistryException(String message) {
			super(message);
		}

		public NoSuchRegistryException(String message, Throwable cause) {
			super(message, cause);
		}

		public NoSuchRegistryException(Throwable cause) {
			super(cause);
		}
	}
}