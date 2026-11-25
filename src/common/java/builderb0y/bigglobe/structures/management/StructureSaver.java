package builderb0y.bigglobe.structures.management;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.mixins.MinecraftServer_SessionAccess;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.StructureSaver.RegionKey;
import builderb0y.bigglobe.structures.management.StructureSaver.StructureRegion;
import builderb0y.bigglobe.structures.management.StructureSaver.StructureStartWrapperWrapper;
import builderb0y.bigglobe.util.SafeCloseable;

public abstract class StructureSaver<
	T_Key extends RegionKey,
	T_Structure extends StructureStartWrapperWrapper,
	T_Region extends StructureRegion<T_Key>
>
implements Runnable, SafeCloseable {

	public static final int VERSION = 0;
	public static final long SAVE_DELAY = Long.getLong("bigglobe.StructureSaver.delay", 2000L);

	public final ServerLevel world;
	public final Path structuresFolder;
	public final InternCache loadedStructures;
	public final LinkedBlockingQueue<T_Region> saveQueue;
	public final Thread workerThread;
	public volatile boolean closing;

	public StructureSaver(ServerLevel world) throws IOException {
		this.world = world;
		this.structuresFolder = ((MinecraftServer_SessionAccess)(world.getServer())).bigglobe_getSession().getDimensionPath(world.dimension()).resolve("bigglobe").resolve("structures");
		if (!Files.isDirectory(this.structuresFolder)) {
			Files.createDirectories(this.structuresFolder);
		}
		this.loadedStructures = new InternCache();
		this.saveQueue = new LinkedBlockingQueue<>();
		this.workerThread = new Thread(this, "Big Globe structure saver thread");
	}

	public void start() {
		this.workerThread.start();
	}

	@Override
	public void close() {
		this.closing = true;
		while (this.workerThread.isAlive()) try {
			LockSupport.unpark(this.workerThread);
			this.workerThread.join(1000L);
		}
		catch (InterruptedException ignored) {}
	}

	@Override
	public void run() {
		while (true) try {
			T_Region region = this.saveQueue.poll();
			if (region != null) {
				long timestamp = region.getSaveTimestamp();
				if (timestamp != StructureRegion.NOT_SCHEDULED) {
					region.setSaveTimestamp(StructureRegion.NOT_SCHEDULED);
					long currentTime = System.currentTimeMillis();
					if (timestamp > currentTime && !this.closing) try {
						Thread.sleep(timestamp - currentTime);
					}
					catch (InterruptedException ignored) {}
					CompoundTag nbt = region.save();
					nbt.putInt("bg_version", VERSION);
					nbt.putInt("mc_version", SharedConstants.getCurrentVersion().dataVersion().version());
					String name = region.getRegionKey().getSaveFileName();
					Path tmp = this.structuresFolder.resolve(name + ".tmp");
					Path real = this.structuresFolder.resolve(name + ".dat");
					NbtIo.writeCompressed(nbt, tmp);
					Files.move(tmp, real, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			else if (this.closing) {
				BigGlobeMod.LOGGER.info(this.workerThread + " shutting down.");
				return;
			}
			else {
				LockSupport.park(this.saveQueue);
			}
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Exception saving structures to disk:", throwable);
		}
	}

	public void markDirty(T_Region region) {
		region.setSaveTimestamp(System.currentTimeMillis() + SAVE_DELAY);
		this.saveQueue.add(region);
		LockSupport.unpark(this.workerThread);
	}

	public @Nullable T_Region get(T_Key key) {
		T_Region region = this.newRegion(key);
		//note: can't read the NBT data from the file on a different thread,
		//because it takes too long. when get() is called, one of the caches
		//will likely be required immediately afterwards, and if reading from
		//a file doesn't finish in time, the result will be re-computed on the CPU.
		//this is both slow, and requires re-saving the file for no reason.
		Path path = this.structuresFolder.resolve(key.getSaveFileName() + ".dat");
		if (Files.exists(path)) try {
			CompoundTag nbt = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
			if (
				nbt.get("bg_version") instanceof NumericTag bgVersion &&
				bgVersion.intValue() == VERSION &&
				nbt.get("mc_version") instanceof NumericTag mcVersion &&
				mcVersion.intValue() == SharedConstants.getCurrentVersion().dataVersion().version()
			) {
				region.load(nbt);
			}
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.warn("Exception reading structures from disk for region " + key, exception);
		}
		return region;
	}

	public abstract T_Region newRegion(T_Key key);

	public abstract T_Structure wrapStructure(StructureStartWrapper start);

	public Map<Holder<Structure>, List<T_Structure>> loadStructures(ListTag layer) {
		return (
			layer
			.stream()
			.map((Tag tag) -> this.loadedStructures.intern((CompoundTag)(tag)))
			.collect(
				Collectors.groupingBy(
					(T_Structure structure) -> (
						structure
						.getWrappedStructureStartWrapper()
						.originalStructure()
					)
				)
			)
		);
	}

	public ListTag saveStructures(Map<Holder<Structure>, List<T_Structure>> structures) {
		StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(this.world);
		return (
			structures
			.values()
			.stream()
			.flatMap(List<T_Structure>::stream)
			.map((T_Structure structure) -> (
				structure
				.getWrappedStructureStartWrapper()
				.toNbt(context)
			))
			.collect(Collectors.toCollection(ListTag::new))
		);
	}

	public static interface RegionKey {

		public abstract String getSaveFileName();
	}

	public static interface StructureStartWrapperWrapper {

		public abstract StructureStartWrapper getWrappedStructureStartWrapper();
	}

	public static interface StructureRegion<T_Key> {

		public static final long NOT_SCHEDULED = Long.MIN_VALUE;

		public abstract T_Key getRegionKey();

		public abstract long getSaveTimestamp();

		public abstract void setSaveTimestamp(long timestamp);

		public abstract CompoundTag save();

		public abstract void load(CompoundTag nbt);
	}

	public class InternCache {

		public final Map<CompoundTag, Entry> map = new ConcurrentHashMap<>();
		public final ReferenceQueue<T_Structure> queue = new ReferenceQueue<>();

		@SuppressWarnings("unchecked")
		public T_Structure intern(CompoundTag nbt) {
			for (Entry entry; (entry = (Entry)(this.queue.poll())) != null;) {
				this.map.remove(entry.key);
			}
			//subtle points:
			//1. we want to hash the NBT tag as few times as possible,
			//which means using compute() instead of get()/put().
			//2. as soon as the value is available, we need to hold a strong reference to it,
			//which means we can't wrap it in an Entry and then unwrap it again,
			//as that would leave it temporarily weakly reachable.
			//we must store it in a strongly-reachable field instead,
			//at least until this method returns.
			var mapper = new BiFunction<CompoundTag, Entry, Entry>() {

				public T_Structure value;

				@Override
				public Entry apply(CompoundTag tag, Entry entry) {
					if (entry != null) {
						T_Structure value = entry.get();
						if (value != null) {
							this.value = value;
							return entry;
						}
					}
					T_Structure value = StructureSaver.this.wrapStructure(
						StructureStartWrapper.fromNbt(
							StructurePieceSerializationContext.fromLevel(
								StructureSaver.this.world
							),
							nbt,
							StructureSaver.this.world.getSeed()
						)
					);
					this.value = value;
					return new Entry(tag, value);
				}
			};
			this.map.compute(nbt, mapper);
			return mapper.value;
		}

		public class Entry extends WeakReference<T_Structure> {

			public final CompoundTag key;

			public Entry(CompoundTag key, T_Structure referent) {
				super(referent);
				this.key = key;
			}
		}
	}
}