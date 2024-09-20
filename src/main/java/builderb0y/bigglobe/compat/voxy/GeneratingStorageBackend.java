package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;

import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.other.DelegatingStorageAdaptor;
import me.cortex.voxy.common.world.WorldEngine;

public class GeneratingStorageBackend extends DelegatingStorageAdaptor {

	public AbstractVoxyWorldGenerator generator;

	public GeneratingStorageBackend(StorageBackend delegate) {
		super(delegate);
	}

	@Override
	public ByteBuffer getSectionData(long key) {
		ByteBuffer data = super.getSectionData(key);
		if (data == null && this.generator != null) {
			System.out.println("Generating " + WorldEngine.getX(key) + ", " + WorldEngine.getY(key) + ", " + WorldEngine.getZ(key) + " at level " + WorldEngine.getLevel(key));
			this.generator.createChunk(key, this);
			data = super.getSectionData(key);
		}
		return data;
	}
}