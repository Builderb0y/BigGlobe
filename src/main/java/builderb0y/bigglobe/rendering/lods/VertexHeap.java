package builderb0y.bigglobe.rendering.lods;

import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.GpuMemory;
import builderb0y.bigglobe.rendering.OutOfVramException;
import builderb0y.bigglobe.util.SafeCloseable;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.system.MemoryUtil.*;

@Environment(EnvType.CLIENT)
public class VertexHeap extends GpuMemory {

	public static final boolean AGGRESSIVE_ASSERTS = false;

	public TreeMapHelper<SliceKey, Slice>
		/** contains only in-use slices. */
		byPosition = new TreeMapHelper<>(SliceKey.POSITION_FIRST),
	/**
	contains only unused slices.
	*/
	bySize = new TreeMapHelper<>(SliceKey.LENGTH_FIRST);

	public VertexHeap(CompactVertexFormat format, long quadCount) {
		super(quadCount * format.byteStride * 4L, GL_ARRAY_BUFFER, GL_ARRAY_BUFFER_BINDING);
		new Slice(this, new SliceKey(0L, this.capacity), false).addToTree();
	}

	@Override
	public void populateInitialData() {
		glBufferData(this.binder, this.capacity, GL_DYNAMIC_DRAW);
	}

	public boolean isEmpty() {
		return this.byPosition.isEmpty();
	}

	public long used() {
		Slice slice = this.byPosition.lastValue();
		return slice != null ? slice.key.end() : 0L;
	}

	public long reallyUsed() {
		return this.byPosition.values().stream().mapToLong((Slice slice) -> slice.key.length).sum();
	}

	public void dump() {
		this.checkThread();
		File root = new File("bigglobe_vertex_heap_dump");
		root.mkdir();
		for (File existing : root.listFiles()) {
			if (existing.getPath().endsWith(".dat") && existing.isFile()) {
				existing.delete();
			}
		}
		if (this.isEmpty()) {
			BigGlobeMod.LOGGER.info("VertexHeap is empty. Nothing to dump.");
			return;
		}
		try (var $ = this.bind()) {
			long heapPointer = nglMapBuffer(this.binder, GL_READ_ONLY);
			if (heapPointer != 0L) try {
				int sliceIndex = 0;
				for (Slice slice : this.byPosition.values()) {
					String fileName = sliceIndex + " [" + slice.key.position() + " + " + slice.key.length + " = " + slice.key.end() + "].dat";
					try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(root, fileName)))) {
						for (long byteIndex = 0; byteIndex < slice.key.length; byteIndex++) {
							stream.write(memGetByte(heapPointer + slice.key.position + byteIndex));
						}
					}
					catch (IOException exception) {
						BigGlobeMod.LOGGER.error("Failed to dump slice " + fileName + ':', exception);
					}
					sliceIndex++;
				}
			}
			finally {
				glUnmapBuffer(this.binder);
			}
			else {
				BigGlobeMod.LOGGER.warn("Failed to map vertex heap for dumping");
			}
		}
	}

	public void assertAggressively() {
		Slice[] allSlices = Stream.concat(this.byPosition.values().stream(), this.bySize.values().stream()).sorted(Comparator.comparing(slice -> slice.key, SliceKey.POSITION_FIRST)).toArray(Slice[]::new);
		for (Slice slice : allSlices) {
			assert (slice.inUse ? this.byPosition : this.bySize).get(slice.key) == slice : "Mismatched slice!";
		}
		assert allSlices[0].key.position() == 0L : "First slice does not start at byte 0";
		for (int index = 0, limit = allSlices.length - 1; index < limit; index++) {
			Slice lower = allSlices[index];
			Slice higher = allSlices[index + 1];
			assert lower.key.end() == higher.key.position() : "Adjacent slices don't touch";
		}
		assert allSlices[allSlices.length - 1].key.end() == this.capacity : "Last slice does not end at capacity";
	}

	public void cleanup() {
		this.checkThread();
		long oldUsed = this.used();
		long currentOffset = 0L;
		for (Slice slice = this.byPosition.firstValue(); slice != null; slice = this.byPosition.higherValue(slice.key)) {
			if (slice.key.position != currentOffset) { //found first slice that needs to be moved.
				this.bySize.clear();
				try (var $ = this.bind()) {
					long heapPointer = nglMapBufferRange(this.binder, currentOffset, this.byPosition.lastKey().end() - currentOffset, GL_MAP_READ_BIT | GL_MAP_WRITE_BIT) - currentOffset;
					if (heapPointer != 0L) try {
						while (slice != null) {
							slice.move(heapPointer, currentOffset);
							currentOffset += slice.key.length;
							slice = this.byPosition.higherValue(slice.key);
						}
					}
					finally {
						if (!glUnmapBuffer(this.binder)) {
							throw new GLException("Failed to unmap buffer");
						}
					}
					else {
						BigGlobeMod.LOGGER.warn("Failed to map vertex heap for de-fragmenting!");
					}
				}
				if (currentOffset != this.capacity) {
					new Slice(this, new SliceKey(currentOffset, this.capacity - currentOffset), false).addToTree();
				}
				break;
			}
			currentOffset += slice.key.length();
		}
		BigGlobeMod.LOGGER.info("LOD VertexHeap de-fragmented " + (oldUsed - currentOffset) + " byte(s): " + oldUsed + " -> " + currentOffset);
		if (AGGRESSIVE_ASSERTS) this.assertAggressively();
	}

	public @Nullable Slice allocate(long address, long size) {
		this.checkThread();
		if (size <= 0L) return null;

		SliceKey replacementKey = new SliceKey(Long.MAX_VALUE, size);
		Slice slice = this.bySize.ceilingValue(replacementKey);
		if (slice == null) {
			this.cleanup();
			slice = this.bySize.ceilingValue(replacementKey);
			if (slice == null) {
				throw new OutOfVramException();
			}
		}

		assert !slice.inUse : "Node in use is in bySize tree!";
		long oldSize = slice.key.length;
		if (oldSize != size) {
			slice.removeFromTree();
			slice.inUse = true;
			slice.key = slice.key.withLength(size);
			slice.addToTree();
			new Slice(this, new SliceKey(slice.key.end(), oldSize - size), false).addToTree();
		}
		else {
			slice.setInUse(true);
		}
		try (var $ = this.bind()) {
			nglBufferSubData(this.binder, slice.key.position, size, address);
		}
		if (AGGRESSIVE_ASSERTS) this.assertAggressively();
		return slice;
	}

	public void markUnused(Slice removed) {
		this.checkThread();
		if (!removed.inUse) return;

		Slice prev = null;
		if (removed.key.position() != 0L) {
			//one of:
			//	used removed ...
			//	unused removed ...
			//	used unused removed...
			prev = this.byPosition.lowerValue(removed.key);
			if (prev == null) {
				//unused removed ...
				prev = this.bySize.get(new SliceKey(0L, removed.key.position()));
				assert prev != null : "Vacuum in vertex heap!";
			}
			else if (prev.inUse) {
				//used removed ...
				//used unused removed ...
				if (prev.key.end() != removed.key.position()) {
					//used unused removed ...
					prev = this.bySize.get(new SliceKey(prev.key.end(), removed.key.position() - prev.key.end()));
					assert prev != null : "Vacuum in vertex heap!";
				}
				else {
					//used removed ...
					prev = null;
				}
			}
		}

		Slice next = null;
		if (removed.key.end() != this.capacity) {
			//one of:
			//	... removed used
			//	... removed unused
			//	... removed unused used
			next = this.byPosition.higherValue(removed.key);
			if (next == null) {
				//... removed unused
				next = this.bySize.get(new SliceKey(removed.key.end(), this.capacity - removed.key.end()));
				assert next != null : "Vacuum in vertex heap!";
			}
			else if (next.inUse) {
				//... removed used
				//... removed unused used
				if (next.key.position() != removed.key.end()) {
					//... removed unused used
					next = this.bySize.get(new SliceKey(removed.key.end(), next.key.position() - removed.key.end()));
					assert next != null : "Vacuum in vertex heap!";
				}
				else {
					//... removed used
					next = null;
				}
			}
		}

		assert prev == null || !prev.inUse;
		assert next == null || !next.inUse;

		long newLength;
		if (prev != null) {
			if (next != null) {
				//... unused removed unused ...
				newLength = next.key.end() - prev.key.position();
				next.removeFromTree();
			}
			else {
				//... unused removed used ...
				newLength = removed.key.end() - prev.key.position();
			}
			removed.removeFromTree();
			prev.removeFromTree();
			prev.key = prev.key.withLength(newLength);
			prev.addToTree();
		}
		else {
			if (next != null) {
				//... used removed unused ...
				newLength = next.key.end() - removed.key.position();
				next.removeFromTree();
				removed.removeFromTree();
				removed.inUse = false;
				removed.key = removed.key.withLength(newLength);
				removed.addToTree();
			}
			else {
				//... used removed used ...
				removed.setInUse(false);
			}
		}
		if (AGGRESSIVE_ASSERTS) this.assertAggressively();
	}

	public static record SliceKey(long position, long length) {

		public static final SliceKey ZERO = new SliceKey(0L, 0L);

		@SuppressWarnings("ComparatorCombinators")
		public static final Comparator<SliceKey>
			POSITION_FIRST = (SliceKey slice1, SliceKey slice2) -> {
			int compare = Long.compare(slice1.position, slice2.position);
			return compare != 0 ? compare : Long.compare(slice1.length, slice2.length);
		},
			LENGTH_FIRST = (SliceKey slice1, SliceKey slice2) -> {
				int compare = Long.compare(slice1.length, slice2.length);
				return compare != 0 ? compare : Long.compare(slice1.position, slice2.position);
			};

		public long end() {
			return this.position + this.length;
		}

		public SliceKey withPosition(long position) {
			return new SliceKey(position, this.length);
		}

		public SliceKey withLength(long length) {
			return new SliceKey(this.position, length);
		}
	}

	public static class Slice implements SafeCloseable {

		public final VertexHeap heap;
		public SliceKey key;
		public boolean inUse;

		@Override
		public String toString() {
			return "Slice[" + this.key.position + " + " + this.key.length + " = " + this.key.end() + "] (" + (this.inUse ? "used" : "unused") + ')';
		}

		public Slice(VertexHeap heap, SliceKey key, boolean inUse) {
			this.heap = heap;
			this.key = key;
			this.inUse = inUse;
		}

		/**
		called during a cleanup.
		*/
		public void move(long heapPointer, long newPosition) {
			assert this.inUse : "Moving non-in-use slice!";
			//assumptions:
			//1. we will be moving at least one quad.
			//2. each quad contains exactly 4 vertices.
			//3. each vertex contains at least one byte of information.
			//because of these assumptions, we can copy 4 bytes at a time.
			//
			//NOTE: do NOT replace this with memCopy(),
			//as the results of memCopy() are undefined when
			//the source and destination ranges overlap.
			for (long offset = 0L, length = this.key.length; offset < length; offset += Integer.BYTES) {
				memPutInt(heapPointer + newPosition + offset, memGetInt(heapPointer + this.key.position + offset));
			}
			this.removeFromTree();
			this.key = this.key.withPosition(newPosition);
			this.addToTree();
		}

		public void setInUse(boolean inUse) {
			if (this.inUse == inUse) return;
			this.removeFromTree();
			this.inUse = inUse;
			this.addToTree();
		}

		@Override
		public void close() {
			this.heap.markUnused(this);
		}

		public void addToTree() {
			(this.inUse ? this.heap.byPosition : this.heap.bySize).put(this.key, this);
		}

		public void removeFromTree() {
			(this.inUse ? this.heap.byPosition : this.heap.bySize).remove(this.key);
		}
	}

	public static class TreeMapHelper<K, V> extends TreeMap<K, V> {

		public TreeMapHelper(Comparator<? super K> comparator) {
			super(comparator);
		}

		public V firstValue() {
			Map.Entry<K, V> entry = this.firstEntry();
			return entry != null ? entry.getValue() : null;
		}

		public V lastValue() {
			Map.Entry<K, V> entry = this.lastEntry();
			return entry != null ? entry.getValue() : null;
		}

		public V lowerValue(K key) {
			Map.Entry<K, V> entry = this.lowerEntry(key);
			return entry != null ? entry.getValue() : null;
		}

		public V floorValue(K key) {
			Map.Entry<K, V> entry = this.floorEntry(key);
			return entry != null ? entry.getValue() : null;
		}

		public V higherValue(K key) {
			Map.Entry<K, V> entry = this.higherEntry(key);
			return entry != null ? entry.getValue() : null;
		}

		public V ceilingValue(K key) {
			Map.Entry<K, V> entry = this.ceilingEntry(key);
			return entry != null ? entry.getValue() : null;
		}
	}
}