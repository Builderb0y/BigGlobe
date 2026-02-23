package builderb0y.bigglobe.rendering.lods;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.compat.voxy.DistanceGraph;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.SafeCloseable;
import builderb0y.bigglobe.util.TimestampedComputingCache;
import builderb0y.bigglobe.util.TimestampedComputingCache.Units;
import builderb0y.bigglobe.util.TimestampedComputingCache.ValueHolder;
import builderb0y.bigglobe.versions.NbtVersions;

public class LodChunkCache implements SafeCloseable {

	public final LodGenerator generator;
	public final ServerWorld world;
	#if MC_VERSION >= MC_1_21_0
		public final net.minecraft.server.world.ServerChunkLoadingManager loader;
	#else
		public final net.minecraft.server.world.ThreadedAnvilChunkStorage loader;
	#endif
	public final TimestampedComputingCache<ChunkPos, LightweightChunk> chunks;
	/**
	querying whether or not a chunk exists is slow,
	and our {@link #chunks} will forget quickly.
	so we have a secondary cache just for chunk existence.
	we start by assuming all chunks are present,
	and mark them as not present when this assumption is invalidated.
	chunks are re-marked as present when they are invalidated
	by {@link #invalidateChunkLater(ChunkPos)}.
	*/
	public final DistanceGraph presentChunks = DistanceGraph.worldOfChunks(true);
	public final ReentrantLock presentChunksLock = new ReentrantLock();
	public final ConcurrentLinkedQueue<ChunkPos> dirtyChunks;
	public final ExecutorService ingester = Executors.newSingleThreadExecutor((Runnable runnable) -> {
		Thread thread = new Thread(runnable, "Big Globe LOD chunk ingest thread");
		thread.setDaemon(true);
		return thread;
	});

	public LodChunkCache(LodGenerator generator, ServerWorld world) {
		this.generator = generator;
		this.world = world;
		#if MC_VERSION >= MC_1_21_0
			this.loader = world.getChunkManager().chunkLoadingManager;
		#else
			this.loader = world.getChunkManager().threadedAnvilChunkStorage;
		#endif
		this.chunks = new TimestampedComputingCache<>(Units.seconds(10.0D), Units.gigabytes(1.0D));
		this.dirtyChunks = new ConcurrentLinkedQueue<>();
	}

	public void invalidateChunkLater(ChunkPos chunkPos) {
		this.dirtyChunks.add(chunkPos);
	}

	public void processDirtyChunks(LodSystem system) {
		if (this.presentChunksLock.tryLock()) try {
			for (ChunkPos chunkPos; (chunkPos = this.dirtyChunks.poll()) != null; ) {
				if (this.chunks.tryInvalidate(chunkPos)) {
					this.presentChunks.set(chunkPos.x, chunkPos.z, true);
					system.invalidateChunkNow(chunkPos);
				}
				else {
					this.dirtyChunks.add(chunkPos);
					break;
				}
			}
		}
		finally {
			this.presentChunksLock.unlock();
		}
	}

	/*
	public LightweightChunk getChunk(ChunkPos chunkPos) {
		return this.chunks.computeIfUnknown(chunkPos, (ChunkPos chunkPos_) -> {
			Optional<NbtCompound> nbt;
			try {
				nbt = this.loader.getUpdatedChunkNbt(chunkPos_).join();
			}
			catch (CancellationException exception) {
				return null;
			}
			catch (CompletionException exception) {
				if (exception.getCause() instanceof CancellationException) return null;
				else throw exception;
			}
			if (nbt.isEmpty()) {
				this.presentChunksLock.lock();
				try {
					this.presentChunks.set(chunkPos_.x, chunkPos_.z, false);
				}
				finally {
					this.presentChunksLock.unlock();
				}
			}
			return nbt.isPresent() ? this.convertChunk(chunkPos_, nbt.get()) : null;
		});
	}
	*/

	public LightweightChunk convertChunk(ChunkPos chunkPos, NbtCompound root) {
		if (
			root.get("Status") instanceof NbtString string &&
			NbtVersions.stringValue(string).equals("minecraft:full") &&
			root.get("sections") instanceof NbtList sectionsNbt
		) {
			BlockSegmentList[] cullingData = (
				BigGlobeConfig.INSTANCE.get().lodRendering.caveCullingDepth >= 0
				&& !this.world.getDimension().hasCeiling()
				? this.generator.generateCullingChunk(chunkPos)
				: null
			);
			LightweightChunk result = new LightweightChunk(this.world, chunkPos);
			result.update(this.world, sectionsNbt, cullingData);
			return result;
		}
		else {
			return null;
		}
	}

	public LightweightChunk[] getChunks(ChunkPos minPos, ChunkPos maxPos) {
		int chunkCount = (maxPos.x - minPos.x) * (maxPos.z - minPos.z);
		ReentrantLock[] lockedHolders = new ReentrantLock[chunkCount];
		CompletableFuture<?>[] futures = new CompletableFuture[chunkCount];
		LightweightChunk[] chunks = new LightweightChunk[chunkCount];
		this.presentChunksLock.lock();
		try {
			int index = 0;
			for (int chunkZ = minPos.z; chunkZ < maxPos.z; chunkZ++) {
				for (int chunkX = minPos.x; chunkX < maxPos.x; chunkX++) {
					if (this.presentChunks.get(chunkX, chunkZ)) {
						final int index_ = index;
						ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
						ValueHolder<LightweightChunk> holder = this.chunks.getHolder(chunkPos, true);
						(lockedHolders[index] = holder.lock).lock();
						if (holder.state != ValueHolder.UNKNOWN) {
							chunks[index] = holder.value;
						}
						else {
							futures[index] = this.loader.getUpdatedChunkNbt(chunkPos).thenApplyAsync(
								(Optional<NbtCompound> optional) -> {
									if (optional.isEmpty()) {
										//remember: this lambda runs in the ingester thread,
										//not the thread that called getChunks().
										//as such, this lock operation and the
										//one outside the loops do not stack.
										this.presentChunksLock.lock();
										try {
											this.presentChunks.set(chunkPos.x, chunkPos.z, false);
										}
										finally {
											this.presentChunksLock.unlock();
										}
									}
									LightweightChunk chunk = optional.isPresent() ? this.convertChunk(chunkPos, optional.get()) : null;
									holder.set(chunk);
									chunks[index_] = chunk;
									if (chunk != null) {
										this.chunks.presentCount.incrementAndGet();
									}
									return null;
								},
								this.ingester
							);
						}
					}
					index++;
				}
			}
		}
		finally {
			//reminder: we must unlock this lock BEFORE joining
			//all the futures or else we will get deadlock.
			this.presentChunksLock.unlock();
			try {
				joinAll(futures);
			}
			finally {
				for (ReentrantLock lock : lockedHolders) {
					if (lock != null) lock.unlock();
				}
			}
		}
		return chunks;
	}

	public static void joinAll(CompletableFuture<?>... futures) {
		CompletionException exception = null;
		for (CompletableFuture<?> future : futures) {
			if (future != null) try {
				future.join();
			}
			catch (Throwable throwable) {
				if (!(throwable instanceof CompletionException && throwable.getCause() instanceof CancellationException)) {
					try {
						if (exception == null) exception = new CompletionException(null);
						exception.addSuppressed(throwable);
					}
					catch (Throwable ignored) {
						//better to swallow the exception than to risk race conditions
						//caused by aborting the enclosing loop too early.
					}
				}
			}
		}
		if (exception != null) throw exception;
	}

	@Override
	public void close() {
		this.chunks.clear();
	}
}