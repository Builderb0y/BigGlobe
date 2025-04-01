package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.cortex.voxy.common.config.section.SectionStorage;
import me.cortex.voxy.common.world.WorldSection;

public class GeneratingStorageBackend extends SectionStorage {

	public final SectionStorage delegate;
	public AbstractVoxyWorldGenerator generator;

	public GeneratingStorageBackend(SectionStorage delegate) {
		this.delegate = delegate;
	}

	@Override
	public int loadSection(WorldSection into) {
		int status = this.delegate.loadSection(into);
		if (status == 1 && this.generator != null) {
			this.generator.queueChunk(into.key);
		}
		return status;
	}

	@Override
	public void saveSection(WorldSection section) {
		this.delegate.saveSection(section);
	}

	@Override
	public void putIdMapping(int id, ByteBuffer data) {
		this.delegate.putIdMapping(id, data);
	}

	@Override
	public Int2ObjectOpenHashMap<byte[]> getIdMappingsData() {
		return this.delegate.getIdMappingsData();
	}

	@Override
	public void iterateStoredSectionPositions(LongConsumer consumer) {
		this.delegate.iterateStoredSectionPositions(consumer);
	}

	@Override
	public void flush() {
		this.delegate.flush();
	}

	@Override
	public void close() {
		this.delegate.close();
	}
}