package builderb0y.bigglobe.scripting.wrappers.entries;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.scripting.wrappers.tags.TagWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class EntryWrapper<T_Raw, T_Tag extends TagWrapper<T_Raw, ?>> {

	public final RegistryEntry<T_Raw> entry;

	public EntryWrapper(RegistryEntry<T_Raw> entry) {
		this.entry = entry;
	}

	public T_Raw object() {
		return this.entry.value();
	}

	public String id() {
		return this.identifier().toString();
	}

	public Identifier identifier() {
		return this.key().getValue();
	}

	public RegistryKey<T_Raw> key() {
		return UnregisteredObjectException.getKey(this.entry);
	}

	public boolean isIn(T_Tag tag) {
		return tag.list.contains(this.entry);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj != null &&
			this.getClass() == obj.getClass() &&
			this.entry == ((EntryWrapper<?, ?>)(obj)).entry
		);
	}

	@Override
	public int hashCode() {
		return this.entry.hashCode();
	}

	@Override
	public String toString() {
		return this.entry.toString();
	}
}