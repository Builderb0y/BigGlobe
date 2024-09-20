package builderb0y.bigglobe.compat.voxy;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.cortex.voxy.common.storage.StorageBackend;
import me.cortex.voxy.common.storage.config.ConfigBuildCtx;
import me.cortex.voxy.common.storage.config.StorageConfig;
import org.lwjgl.system.*;

import builderb0y.bigglobe.noise.Permuter;

public class ForgetfulMemoryStorageBackend extends StorageBackend implements QueueingStorageBackend {

	public static final int MAP_COUNT = 16;
	public static final long RETENTION_MILLISECONDS = 60L * 1000L;

	public final SectionDataMap[] maps;
	public final Int2ObjectOpenHashMap<ByteBuffer> idMappings;
	public GenerationQueue queue;

	public ForgetfulMemoryStorageBackend() {
		this.maps = new SectionDataMap[MAP_COUNT];
		for (int index = 0; index < MAP_COUNT; index++) {
			this.maps[index] = new SectionDataMap();
		}
		this.idMappings = new Int2ObjectOpenHashMap<>();
	}

	public SectionDataMap getMap(long key) {
		return this.maps[((int)(Permuter.stafford(key))) & (MAP_COUNT - 1)];
	}

	public void clean(SectionDataMap map) {
		assert Thread.holdsLock(map);
		long deadline = System.currentTimeMillis() - RETENTION_MILLISECONDS;
		while (!map.isEmpty() && map.firstValue().timestamp < deadline) {
			this.queue.clear(map.firstLongKey() & 0xF00F_FFFF_FFFF_FFFFL);
			MemoryUtil.memFree(map.removeFirst().buffer);
		}
	}

	@Override
	public ByteBuffer getSectionData(long key) {
		SectionDataMap map = this.getMap(key);
		synchronized (map) {
			TimestampedByteBuffer data = map.getAndMoveToLast(key);
			if (data != null) {
				data.timestamp = System.currentTimeMillis();
			}
			this.clean(map);
			if (data != null) {
				ByteBuffer copy = MemoryUtil.memAlloc(data.buffer.remaining());
				MemoryUtil.memCopy(data.buffer, copy);
				return copy;
			}
			else {
				this.queue.add(key & 0xF00F_FFFF_FFFF_FFFFL);
				return null;
			}
		}
	}

	@Override
	public void setSectionData(long key, ByteBuffer newData) {
		SectionDataMap map = this.getMap(key);
		synchronized (map) {
			TimestampedByteBuffer data = map.getAndMoveToLast(key);
			if (data != null) {
				data.timestamp = System.currentTimeMillis();
			}
			this.clean(map);
			if (data != null) {
				if (data.buffer.capacity() >= newData.remaining()) {
					MemoryUtil.memCopy(newData, data.buffer.clear());
					data.buffer.limit(newData.remaining());
				}
				else {
					ByteBuffer copy = MemoryUtil.memAlloc(newData.remaining());
					MemoryUtil.memCopy(newData, copy);
					MemoryUtil.memFree(data.buffer);
					data.buffer = copy;
				}
			}
			else {
				ByteBuffer copy = MemoryUtil.memAlloc(newData.remaining());
				MemoryUtil.memCopy(newData, copy);
				map.putAndMoveToLast(key, new TimestampedByteBuffer(copy));
			}
		}
	}

	@Override
	public void deleteSectionData(long key) {
		SectionDataMap map = this.getMap(key);
		synchronized (map) {
			map.remove(key);
			this.clean(map);
		}
	}

	@Override
	public void putIdMapping(int id, ByteBuffer newData) {
		synchronized (this.idMappings) {
			ByteBuffer oldData = this.idMappings.get(id);
			if (oldData != null) {
				if (oldData.capacity() >= newData.remaining()) {
					MemoryUtil.memCopy(newData, oldData.clear());
					oldData.limit(newData.remaining());
				}
				else {
					ByteBuffer copy = MemoryUtil.memAlloc(newData.remaining());
					MemoryUtil.memCopy(newData, copy);
					MemoryUtil.memFree(oldData);
					this.idMappings.put(id, copy);
				}
			}
			else {
				ByteBuffer copy = MemoryUtil.memAlloc(newData.remaining());
				MemoryUtil.memCopy(newData, copy);
				this.idMappings.put(id, copy);
			}
		}
	}

	@Override
	public Int2ObjectOpenHashMap<byte[]> getIdMappingsData() {
		synchronized (this.idMappings) {
			Int2ObjectOpenHashMap<byte[]> result = new Int2ObjectOpenHashMap<>(this.idMappings.size());
			for (
				ObjectIterator<Int2ObjectMap.Entry<ByteBuffer>> iterator = this.idMappings.int2ObjectEntrySet().fastIterator();
				iterator.hasNext();
			) {
				Int2ObjectMap.Entry<ByteBuffer> entry = iterator.next();
				ByteBuffer buffer = entry.getValue();
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				buffer.rewind();
				result.put(entry.getIntKey(), bytes);
			}
			return result;
		}
	}

	@Override
	public void flush() {

	}

	@Override
	public void close() {
		for (int mapIndex = 0; mapIndex < MAP_COUNT; mapIndex++) {
			SectionDataMap map = this.maps[mapIndex];
			synchronized (map) {
				for (TimestampedByteBuffer data : map.values()) {
					MemoryUtil.memFree(data.buffer);
				}
				map.clear();
			}
		}
		synchronized (this.idMappings) {
			for (ByteBuffer buffer : this.idMappings.values()) {
				MemoryUtil.memFree(buffer);
			}
		}
	}

	@Override
	public GenerationQueue getQueue() {
		return this.queue;
	}

	@Override
	public void setQueue(GenerationQueue queue) {
		this.queue = queue;
	}

	public static class TimestampedByteBuffer {

		public ByteBuffer buffer;
		public long timestamp;

		public TimestampedByteBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
			this.timestamp = System.currentTimeMillis();
		}
	}

	public static class SectionDataMap extends Long2ObjectLinkedOpenHashMap<TimestampedByteBuffer> {

		public SectionDataMap() {}

		public SectionDataMap(int expected) {
			super(expected);
		}

		public TimestampedByteBuffer firstValue() {
			if (this.size == 0) throw new NoSuchElementException();
			//this.value.as(Object[])[this.first].as(TimestampedByteBuffer)
			return (
				(TimestampedByteBuffer)(
					(
						(Object[])(
							this.value
						)
					)
					[this.first]
				)
			);
		}
	}

	public static class Config extends StorageConfig {

		@Override
		public StorageBackend build(ConfigBuildCtx context) {
			return new ForgetfulMemoryStorageBackend();
		}

		public static String getConfigTypeName() {
			return "BigGlobe_ForgetfulMemory";
		}
	}
}