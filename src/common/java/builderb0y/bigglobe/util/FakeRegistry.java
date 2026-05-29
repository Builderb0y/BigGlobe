package builderb0y.bigglobe.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableObject;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class FakeRegistry<T> {

	public final ResourceKey<Registry<T>> key;
	public final HolderOwner<T> owner = new HolderOwner<>() {};
	public final ReferenceQueue<Holder<T>> queue = new ReferenceQueue<>();
	public final ConcurrentHashMap<T, WeakReference<Holder<T>>> cache = new ConcurrentHashMap<>(32);

	public FakeRegistry(Identifier key) {
		this.key = ResourceKey.createRegistryKey(key);
	}

	public FakeRegistry(ResourceKey<Registry<T>> key) {
		this.key = key;
	}

	public synchronized Holder<T> getOrCreate(Identifier id, T object) {
		class Ref extends WeakReference<Holder<T>> {

			public final T key;

			public Ref(Holder<T> entry) {
				super(entry, FakeRegistry.this.queue);
				this.key = entry.value();
			}
		}
		for (Reference<? extends Holder<T>> reference; (reference = this.queue.poll()) != null; ) {
			this.cache.remove(((Ref)(reference)).key);
		}
		MutableObject<Holder<T>> result = new MutableObject<>();
		this.cache.compute(
			object, (T value, WeakReference<Holder<T>> ref) -> {
				Holder<T> entry;
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
			}
		);
		return result.getValue();
	}

	public static class RegistryEntryImpl<T> extends Holder.Reference<T> {

		public RegistryEntryImpl(HolderOwner<T> owner, ResourceKey<Registry<T>> key, Identifier id, T value) {
			super(
				Holder.Reference.Type.STAND_ALONE,
				owner,
				ResourceKey.create(key, id),
				value
			);
		}
	}
}