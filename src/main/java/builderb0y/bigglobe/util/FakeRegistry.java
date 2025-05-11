package builderb0y.bigglobe.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableObject;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.util.Identifier;

public class FakeRegistry<T> {

	public final RegistryKey<Registry<T>> key;
	public final RegistryEntryOwner<T> owner = new RegistryEntryOwner<>() {};
	public final ReferenceQueue<RegistryEntry<T>> queue = new ReferenceQueue<>();
	public final ConcurrentHashMap<T, WeakReference<RegistryEntry<T>>> cache = new ConcurrentHashMap<>(32);

	public FakeRegistry(Identifier key) {
		this.key = RegistryKey.ofRegistry(key);
	}

	public FakeRegistry(RegistryKey<Registry<T>> key) {
		this.key = key;
	}

	public synchronized RegistryEntry<T> getOrCreate(Identifier id, T object) {
		class Ref extends WeakReference<RegistryEntry<T>> {

			public final T key;

			public Ref(RegistryEntry<T> entry) {
				super(entry, FakeRegistry.this.queue);
				this.key = entry.value();
			}
		}
		for (Reference<? extends RegistryEntry<T>> reference; (reference = this.queue.poll()) != null;) {
			this.cache.remove(((Ref)(reference)).key);
		}
		MutableObject<RegistryEntry<T>> result = new MutableObject<>();
		this.cache.compute(object, (T value, WeakReference<RegistryEntry<T>> ref) -> {
			RegistryEntry<T> entry;
			if (ref != null) {
				entry = ref.get();
				if (entry != null) {
					result.setValue(entry);
					return ref;
				}
			}
			entry = new RegistryEntryImpl<>(this.owner, this.key, id, value);
			result.setValue(entry);
			return new Ref(entry);
		});
		return result.getValue();
	}

	public static class RegistryEntryImpl<T> extends RegistryEntry.Reference<T> {

		public RegistryEntryImpl(RegistryEntryOwner<T> owner, RegistryKey<Registry<T>> key, Identifier id, T value) {
			super(
				RegistryEntry.Reference.Type.STAND_ALONE,
				owner,
				RegistryKey.of(key, id),
				value
			);
		}
	}
}