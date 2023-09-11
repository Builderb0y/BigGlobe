package builderb0y.bigglobe.columns;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.mixinInterfaces.ChunkOfColumnsHolder;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.util.SemiThreadLocal;
import builderb0y.bigglobe.util.Tripwire;

public class ChunkOfColumnsRecycler {

	public static final int RECYCLER_SIZE = Integer.getInteger(BigGlobeMod.MODID + ".columnRecyclerSize", 256);

	public final BigGlobeChunkGenerator generator;
	public final ReferenceQueue<Chunk> queue;
	public final Set<Ref> tracking;
	public final SemiThreadLocal<ChunkOfColumns<? extends WorldColumn>> available;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ChunkOfColumnsRecycler(BigGlobeChunkGenerator generator) {
		this.generator = generator;
		this.queue = new ReferenceQueue<>();
		this.tracking = new ReferenceOpenHashSet<>(RECYCLER_SIZE);
		this.available = SemiThreadLocal.soft(RECYCLER_SIZE, () -> new ChunkOfColumns(generator::column));
	}

	public @Nullable ChunkOfColumns<? extends WorldColumn> pollQueue() {
		assert Thread.holdsLock(this);
		Ref ref = (Ref)(this.queue.poll());
		if (ref != null) {
			this.tracking.remove(ref);
			return ref.columns;
		}
		return null;
	}

	public synchronized void processQueue() {
		ChunkOfColumns<? extends WorldColumn> columns = this.pollQueue();
		if (columns != null) {
			ObjectArrayList<ChunkOfColumns<? extends WorldColumn>> list = new ObjectArrayList<>(RECYCLER_SIZE);
			list.add(columns);
			for (int index = 1; index < RECYCLER_SIZE; index++) {
				columns = this.pollQueue();
				if (columns != null) {
					list.add(columns);
				}
				else {
					break;
				}
			}
			this.available.reclaimAll(list);
			while (this.queue.poll() != null) {}
		}
	}

	public ChunkOfColumns<? extends WorldColumn> get(Chunk chunk, ScriptStructures structures, boolean distantHorizons) {
		this.processQueue();
		ChunkOfColumns<? extends WorldColumn> columns;
		if (chunk instanceof ChunkOfColumnsHolder holder) {
			columns = holder.bigglobe_getChunkOfColumns();
			if (columns == null) {
				columns = this.available.get();
				this.generator.populateChunkOfColumns(columns, chunk.getPos(), structures, distantHorizons);
				holder.bigglobe_setChunkOfColumns(columns);
				Ref ref = new Ref(chunk, this.queue, columns);
				synchronized (this) {
					this.tracking.add(ref);
				}
			}
		}
		else {
			if (Tripwire.isEnabled()) {
				Tripwire.logWithStackTrace("Chunk is not a ChunkOfColumnsHolder: " + chunk);
			}
			columns = this.available.get();
			this.generator.populateChunkOfColumns(columns, chunk.getPos(), structures, distantHorizons);
		}
		return columns;
	}

	public static class Ref extends PhantomReference<Chunk> {

		public final ChunkOfColumns<? extends WorldColumn> columns;

		public Ref(Chunk chunk, ReferenceQueue<? super Chunk> queue, ChunkOfColumns<? extends WorldColumn> columns) {
			super(chunk, queue);
			this.columns = columns;
		}
	}
}