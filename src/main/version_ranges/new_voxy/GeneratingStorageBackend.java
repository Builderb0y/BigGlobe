package builderb0y.bigglobe.compat.voxy;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.other.DelegatingStorageAdaptor;
import me.cortex.voxy.common.util.MemoryBuffer;

public class GeneratingStorageBackend extends DelegatingStorageAdaptor implements QueueingStorageBackend {

	public AbstractVoxyWorldGenerator generator;

	public GeneratingStorageBackend(StorageBackend delegate) {
		super(delegate);
	}

	@Override
	public MemoryBuffer getSectionData(long key, MemoryBuffer scratch) {
		MemoryBuffer data = super.getSectionData(key, scratch);
		if (data == null && this.generator != null) {
			data = this.generator.generateNextChunk(key);
			if (data != null) {
				data.cpyTo(scratch.address);
				data.free();
				return scratch.subSize(data.size);
			}
		}
		return data;
	}

	@Override
	public AbstractVoxyWorldGenerator getGenerator() {
		return this.generator;
	}

	@Override
	public void setGenerator(AbstractVoxyWorldGenerator generator) {
		this.generator = generator;
	}
}