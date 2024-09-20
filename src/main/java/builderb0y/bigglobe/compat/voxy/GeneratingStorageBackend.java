package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.other.DelegatingStorageAdaptor;

import builderb0y.bigglobe.noise.Permuter;

public class GeneratingStorageBackend extends DelegatingStorageAdaptor {

	public AbstractVoxyWorldGenerator generator;
	public final Long2ObjectMap<Boolean>[] seen;

	@SuppressWarnings("unchecked") //generic array.
	public GeneratingStorageBackend(StorageBackend delegate) {
		super(delegate);
		this.seen = new Long2ObjectMap[16];
		for (int index = 0; index < 16; index++) {
			this.seen[index] = new Long2ObjectOpenHashMap<>(1024);
		}
	}

	@Override
	public ByteBuffer getSectionData(long key) {
		ByteBuffer data = super.getSectionData(key);
		if (data == null && this.generator != null) {
			long constantYKey = key & 0xF00F_FFFF_FFFF_FFFFL; //"foof" is fun to say.
			Long2ObjectMap<Boolean> map = this.seen[Long.hashCode(Permuter.stafford(constantYKey)) & 15];
			synchronized (map) {
				while (true) {
					Boolean old = map.putIfAbsent(constantYKey, Boolean.FALSE);
					if (old == null) {
						this.generator.createChunk(key, this);
						map.put(constantYKey, Boolean.TRUE);
						map.notifyAll();
						break;
					}
					else if (old == Boolean.FALSE) try {
						map.wait();
					}
					catch (InterruptedException exception) {
						return null;
					}
					else {
						break;
					}
				}
			}
			data = super.getSectionData(key);
		}
		return data;
	}
}