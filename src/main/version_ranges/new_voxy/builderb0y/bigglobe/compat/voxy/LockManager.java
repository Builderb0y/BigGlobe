package builderb0y.bigglobe.compat.voxy;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class LockManager {

	public final Long2ObjectMap<Lock> locks = new Long2ObjectOpenHashMap<>(Runtime.getRuntime().availableProcessors());

	public boolean tryBeginChunk(long pos) {
		pos &= 0xF00F_FFFF_FFFF_FFFFL;
		Lock lock;
		synchronized (this) {
			lock = this.locks.get(pos);
			if (lock == null) {
				this.locks.put(pos, new Lock());
				return true;
			}
		}
		lock.waitUntilDone();
		return false;
	}

	public void finishChunk(long pos) {
		pos &= 0xF00F_FFFF_FFFF_FFFFL;
		Lock lock;
		synchronized (this) {
			lock = this.locks.remove(pos);
		}
		if (lock != null) lock.setDone();
	}

	public static class Lock {

		public volatile boolean done;

		public synchronized void setDone() {
			this.done = true;
			this.notifyAll();
		}

		public synchronized void waitUntilDone() {
			try {
				while (!this.done) {
					this.wait();
				}
			}
			catch (InterruptedException ignored) {}
		}
	}
}