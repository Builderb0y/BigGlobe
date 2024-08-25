package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.other.DelegatingStorageAdaptor;

public class GeneratingStorageBackend extends DelegatingStorageAdaptor implements QueueingStorageBackend {

	public GenerationQueue queue;

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

	@Override
	public GenerationQueue getQueue() {
		return this.queue;
	}

	@Override
	public void setQueue(GenerationQueue queue) {
		this.queue = queue;
	}
}