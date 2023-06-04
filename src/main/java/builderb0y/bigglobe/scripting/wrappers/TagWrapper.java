package builderb0y.bigglobe.scripting.wrappers;

import java.util.Iterator;
import java.util.Optional;
import java.util.random.RandomGenerator;

import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.noise.MojangPermuter;

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
		Optional<RegistryEntryList.Named<T_Raw>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(key.registry()).getEntryList(key);
		if (list.isEmpty()) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " does not exist");
		Optional<RegistryEntry<T_Raw>> element = list.get().getRandom(new MojangPermuter(random.nextLong()));
		if (element.isEmpty()) throw new RuntimeException("#" + key.registry().getValue() + " / " + key.id() + " is empty");
		return this.wrap(element.get());
	}

	@Override
	public default Iterator<T_Entry> iterator() {
		Optional<RegistryEntryList.Named<T_Raw>> list = BigGlobeMod.getCurrentServer().getRegistryManager().get(this.key().registry()).getEntryList(this.key());
		if (list.isEmpty()) throw new RuntimeException("#" + this.key().registry().getValue() + " / " + this.key().id() + " does not exist");
		return list.get().stream().map(this::wrap).iterator();
	}
}