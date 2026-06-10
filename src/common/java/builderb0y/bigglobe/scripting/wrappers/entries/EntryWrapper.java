package builderb0y.bigglobe.scripting.wrappers.entries;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import builderb0y.bigglobe.scripting.wrappers.tags.TagWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

public class EntryWrapper<T_Raw, T_Tag extends TagWrapper<T_Raw, ?>> {

	public static final TypeInfo TYPE = TypeInfo.of(EntryWrapper.class);

	public final Holder<T_Raw> entry;

	public EntryWrapper(Holder<T_Raw> entry) {
		this.entry = entry;
	}

	public T_Raw object() {
		return this.entry.value();
	}

	public String id() {
		return this.identifier().toString();
	}

	public Identifier identifier() {
		return this.key().identifier();
	}

	public ResourceKey<T_Raw> key() {
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