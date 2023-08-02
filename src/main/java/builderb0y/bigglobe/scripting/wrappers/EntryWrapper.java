package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.util.UnregisteredObjectException;

public interface EntryWrapper<T_Raw, T_TagWrapper extends TagWrapper<T_Raw, ?>> {

	public abstract RegistryEntry<T_Raw> entry();

	public default T_Raw object() {
		return this.entry().value();
	}

	public default String id() {
		return this.identifier().toString();
	}

	public default RegistryKey<T_Raw> key() {
		return UnregisteredObjectException.getKey(this.entry());
	}

	public default Identifier identifier() {
		return this.key().getValue();
	}

	public abstract boolean isIn(T_TagWrapper tag);

	public default boolean isInImpl(T_TagWrapper tag) {
		return this.entry().isIn(tag.key());
	}
}