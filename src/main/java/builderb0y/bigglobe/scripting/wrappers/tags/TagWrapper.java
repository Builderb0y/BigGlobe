package builderb0y.bigglobe.scripting.wrappers.tags;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.InfoHolder;

public abstract class TagWrapper<T_Raw, T_Entry> implements Iterable<T_Entry> {

	public static final TypeInfo TYPE = TypeInfo.of(TagWrapper.class);

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo isEmpty, size, contains;
		@Disambiguate(name = "random", returnType = Object.class, paramTypes = { RandomGenerator.class })
		public MethodInfo rngRandom;
		@Disambiguate(name = "random", returnType = Object.class, paramTypes = { long.class})
		public MethodInfo seedRandom;
	}

	public final DelayedEntryList<T_Raw> list;

	public TagWrapper(DelayedEntryList<T_Raw> list) {
		this.list = list;
	}

	public abstract T_Entry wrap(RegistryEntry<T_Raw> entry);

	public abstract RegistryEntry<T_Raw> unwrap(T_Entry entry);

	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	public int size() {
		return this.list.size();
	}

	public T_Entry random(RandomGenerator random) {
		return this.wrap(this.list.randomEntry(random));
	}

	public T_Entry random(long seed) {
		return this.wrap(this.list.randomEntry(seed));
	}

	public boolean contains(T_Entry entry) {
		return this.list.contains(this.unwrap(entry));
	}

	@Override
	public @NotNull Iterator<T_Entry> iterator() {
		return this.list.entryStream().map(this::wrap).iterator();
	}

	@Override
	public String toString() {
		return this.list.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj != null &&
			this.getClass() == obj.getClass() &&
			this.list.equals(((TagWrapper<?, ?>)(obj)).list)
		);
	}

	@Override
	public int hashCode() {
		return this.list.hashCode();
	}
}