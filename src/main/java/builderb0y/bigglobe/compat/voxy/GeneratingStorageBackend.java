package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.other.DelegatingStorageAdaptor;

public class GeneratingStorageBackend extends DelegatingStorageAdaptor {

	public GenerationProgressTracker queue;

	public GeneratingStorageBackend(StorageBackend delegate) {
		super(delegate);
	}

	@Override
	public ByteBuffer getSectionData(long key) {
		ByteBuffer data = super.getSectionData(key);
		if (data == null) {
			this.queue.add(key & 0xF00F_FFFF_FFFF_FFFFL);
		}
		return data;
	}

	public static class GenerationProgressTracker {

		public final LongSortedSet queue = new LongAVLTreeSet(Long::compareUnsigned);
		public final LongSet seen = new LongOpenHashSet();

		public synchronized void add(long key) {
			if (this.seen.add(key)) {
				this.queue.add(key);
			}
		}

		public synchronized long poll() {
			if (this.queue.isEmpty()) return -1L;
			long result = this.queue.firstLong();
			this.queue.remove(result);
			return result;
		}
	}
}