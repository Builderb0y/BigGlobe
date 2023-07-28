package builderb0y.bigglobe.dynamicRegistries;

import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.tag.TagKey;

import builderb0y.bigglobe.versions.RegistryVersions;

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
			return this.registry.getOrCreateEntry(key);
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

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> castStream(Stream<? extends T> stream) {
		return (Stream<T>)(stream);
	}
}