package builderb0y.bigglobe.registry;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.util.UnregisteredObjectException;

public class BetterRegistryEntry<T> {

	public final RegistryEntry<T> vanilla;
	public final RegistryKey<T> key;
	public final T object;
	public Set<TagKey<T>> tags;

	public BetterRegistryEntry(RegistryEntry<T> vanilla, RegistryKey<T> key, T object) {
		this.vanilla = vanilla;
		this.key = key;
		this.object = object;
	}

	public static <T> BetterRegistryEntry<T> from(RegistryEntry<T> vanilla) {
		return new BetterRegistryEntry<>(vanilla, UnregisteredObjectException.getKey(vanilla), vanilla.value());
	}

	public RegistryEntry<T> vanilla() {
		return this.vanilla;
	}

	public RegistryKey<T> key() {
		return this.key;
	}

	public T object() {
		return this.object;
	}

	public Set<TagKey<T>> tags() {
		if (this.tags == null) {
			this.tags = this.vanilla.streamTags().collect(Collectors.toSet());
		}
		return this.tags;
	}

	public Identifier id() {
		return this.key.getValue();
	}

	public boolean isIn(TagKey<T> tagKey) {
		return this.vanilla.isIn(tagKey);
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj instanceof BetterRegistryEntry<?> that && this.key.equals(that.key));
	}

	@Override
	public String toString() {
		return "BetterRegistryEntry: { key: " + this.key + ", object: " + this.object + " }";
	}
}