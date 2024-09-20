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
		if (data == null && this.queue != null) {
			this.queue.queueChunk(key);
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