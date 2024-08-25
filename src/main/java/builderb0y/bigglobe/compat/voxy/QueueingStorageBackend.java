package builderb0y.bigglobe.compat.voxy;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

public interface QueueingStorageBackend {

	public abstract GenerationQueue getQueue();

	public abstract void setQueue(GenerationQueue queue);

	public static class GenerationQueue {

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

		public synchronized void clear(long key) {
			this.seen.remove(key);
		}
	}
}