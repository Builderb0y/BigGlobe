package builderb0y.bigglobe.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.bigglobe.noise.MojangPermuter;

public class TagOrObject<T> implements Iterable<RegistryEntry<T>> {

	public final @Nullable RegistryEntryList<T> tag;
	public final @Nullable RegistryEntry<T> object;

	public TagOrObject(@NotNull RegistryEntryList<T> tag) {
		this.tag = tag;
		this.object = null;
	}

	public TagOrObject(@NotNull RegistryEntry<T> object) {
		this.tag = null;
		this.object = object;
	}

	public boolean contains(RegistryEntry<T> entry) {
		return this.tag != null ? this.tag.contains(entry) : this.object.matchesKey(UnregisteredObjectException.getKey(entry));
	}

	public boolean isEmpty() {
		return this.tag != null && this.tag.size() == 0;
	}

	public RegistryEntry<T> random(RandomGenerator random) {
		return this.tag != null ? this.tag.getRandom(MojangPermuter.from(random)).orElse(null) : this.object;
	}

	@Override
	public Iterator<RegistryEntry<T>> iterator() {
		return this.tag != null ? this.tag.iterator() : Collections.singletonList(this.object).iterator();
	}

	@Override
	public void forEach(Consumer<? super RegistryEntry<T>> action) {
		if (this.tag != null) this.tag.forEach(action);
		else action.accept(this.object);
	}

	@Override
	public Spliterator<RegistryEntry<T>> spliterator() {
		return this.tag != null ? this.tag.spliterator() : Collections.singletonList(this.object).spliterator();
	}

	public Stream<RegistryEntry<T>> stream() {
		return this.tag != null ? this.tag.stream() : Stream.of(this.object);
	}

	public <X extends Throwable> String toString(Function<String, X> exceptionFactory) throws X {
		if (this.tag != null) {
			TagKey<T> key = this.tag.getStorage().left().orElse(null);
			if (key != null) {
				return "#" + key.id();
			}
			else {
				throw exceptionFactory.apply("Tag " + this.tag + " is missing a key");
			}
		}
		else {
			RegistryKey<T> key = this.object.getKey().orElse(null);
			if (key != null) {
				return key.getValue().toString();
			}
			else {
				throw exceptionFactory.apply("RegistryEntry " + this.object + " is missing a key");
			}
		}
	}
}