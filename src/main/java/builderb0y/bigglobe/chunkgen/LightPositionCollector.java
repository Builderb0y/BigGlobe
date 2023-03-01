package builderb0y.bigglobe.chunkgen;

import java.util.*;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ProtoChunk;

/**
efficient temporary storage for 0-4096 BlockPos's inside the same ChunkSection volume.
this is used to coordinate lighting updates so that light-emitting
blocks can be placed asynchronously, but the positions of those
blocks can be added to {@link ProtoChunk#lightSources} synchronously.
*/
public class LightPositionCollector implements Iterable<BlockPos> {

	public int startX, startY, startZ;
	public BitSet bits;

	public LightPositionCollector(int startX, int startY, int startZ) {
		this.startX = startX;
		this.startY = startY;
		this.startZ = startZ;
	}

	public void add(int index) {
		if (this.bits == null) {
			this.bits = new BitSet(4096);
		}
		this.bits.set(index);
	}

	@Override
	public Iterator<BlockPos> iterator() {
		BitSet bits = this.bits;
		if (bits == null) return Collections.emptyIterator();
		int startX = this.startX, startY = this.startY, startZ = this.startZ;
		return new Iterator<>() {

			public int nextIndex = bits.nextSetBit(0);

			@Override
			public boolean hasNext() {
				return this.nextIndex >= 0;
			}

			@Override
			public BlockPos next() {
				int index = this.nextIndex;
				if (index < 0) throw new NoSuchElementException();
				this.nextIndex = bits.nextSetBit(index + 1);
				return new BlockPos(startX | (index & 15), startY | (index >>> 8), startZ | ((index >>> 4) & 15));
			}
		};
	}

	public int size() {
		return this.bits != null ? this.bits.cardinality() : 0;
	}

	public boolean isEmpty() {
		return this.bits == null || this.bits.isEmpty();
	}
}