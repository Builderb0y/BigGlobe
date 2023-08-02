package builderb0y.bigglobe.scripting.wrappers;

import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.Permuter;

public interface TagWrapper<T_Raw, T_Entry> extends Iterable<T_Entry> {

	public abstract TagKey<T_Raw> key();

	public abstract T_Entry wrap(RegistryEntry<T_Raw> entry);

	/**
	implementations should delegate to {@link #randomImpl(RandomGenerator)}.
	the reason why we have 2 methods here instead of just making random()
	default is to ensure that on subclasses, a method which returns
	the actual type of T_Entry instead of the generic type exists.
	the actual type is used by scripting, so the bytecode is a bit picky.
	*/
	public abstract T_Entry random(RandomGenerator random);

	public default T_Entry randomImpl(RandomGenerator random) {
		TagKey<T_Raw> key = this.key();
		RegistryEntryList.Named<T_Raw> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(key.registry()).getEntryList(key).orElse(null);
		if (list == null) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " does not exist.");
		if (list.size() == 0) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " is empty.");
		RegistryEntry<T_Raw> element = list.get(random.nextInt(list.size()));
		return this.wrap(element);
	}

	public abstract T_Entry random(long seed);

	public default T_Entry randomImpl(long seed) {
		TagKey<T_Raw> key = this.key();
		RegistryEntryList.Named<T_Raw> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(key.registry()).getEntryList(key).orElse(null);
		if (list == null) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " does not exist.");
		if (list.size() == 0) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " is empty.");
		RegistryEntry<T_Raw> element = list.get(Permuter.nextBoundedInt(seed, list.size()));
		return this.wrap(element);
	}

	@Override
	public default Iterator<T_Entry> iterator() {
		Optional<RegistryEntryList.Named<T_Raw>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(this.key().registry()).getEntryList(this.key());
		if (list.isEmpty()) throw new RuntimeException("#" + this.key().registry().getValue() + " / " + this.key().id() + " does not exist");
		return list.get().stream().map(this::wrap).iterator();
	}
}