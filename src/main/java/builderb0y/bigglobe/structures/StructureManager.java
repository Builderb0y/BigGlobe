package builderb0y.bigglobe.structures;

import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.*;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.compat.ValkyrienSkiesCompat;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class StructureManager {

	public final WorldUngeneratedStructures worldUngeneratedStructures = new WorldUngeneratedStructures(60_000);

	public static record StructureGenerationParams(
		BigGlobeScriptedChunkGenerator generator,
		StructurePlacementCalculator structurePlacementCalculator,
		DynamicRegistryManager dynamicRegistries,
		NoiseConfig noiseConfig,
		StructureTemplateManager structureTemplateManager,
		HeightLimitView heightLimitView,
		ChunkPos chunkPos
	) {

		public BiomeSource biomeSource() {
			return this.generator.biome_source();
		}

		public long columnSeed() {
			return this.generator.columnSeed;
		}

		public Structure.Context toStructureContext(Predicate<RegistryEntry<Biome>> predicate) {
			return new Structure.Context(
				this.dynamicRegistries,
				this.generator,
				this.generator.biome_source(),
				this.noiseConfig,
				this.structureTemplateManager,
				this.columnSeed(),
				this.chunkPos,
				this.heightLimitView,
				predicate
			);
		}

		public StructureGenerationParams at(int x, int z) {
			return this.at(new ChunkPos(x, z));
		}

		public StructureGenerationParams at(ChunkPos pos) {
			return new StructureGenerationParams(
				this.generator,
				this.structurePlacementCalculator,
				this.dynamicRegistries,
				this.noiseConfig,
				this.structureTemplateManager,
				this.heightLimitView,
				pos
			);
		}
	}

	public synchronized void setStructureStarts(StructureGenerationParams params, Chunk chunk) {
		if (!chunk.getStructureStarts().isEmpty()) {
			BigGlobeMod.LOGGER.warn(chunk + " already has structure starts");
			return;
		}
		ChunkUngeneratedStructures toAdd = this.getStructureStarts(params);
		outer:
		if (!toAdd.isEmpty()) {
			toAdd = new ChunkUngeneratedStructures(toAdd);
			for (int offsetZ = -8; offsetZ <= 8; offsetZ++) {
				for (int offsetX = -8; offsetX <= 8; offsetX++) {
					if (offsetX == 0 && offsetZ == 0) continue;
					StructureGenerationParams params2 = params.at(
						params.chunkPos.x + offsetX,
						params.chunkPos.z + offsetZ
					);
					toAdd.removeIntersecting(this.getStructureStarts(params2));
					if (toAdd.isEmpty()) break outer;
				}
			}
			Map<Structure, StructureStart> map = new HashMap<>(toAdd.size());
			for (SortedStructurePieces pieces : toAdd) {
				//System.out.println("Survivor: " + toString(pieces.getStart()));
				map.merge(pieces.getStart().getStructure(), pieces.getStart(), (StructureStart start1, StructureStart start2) -> {
					//todo: handle multiple of the same structure in the same chunk.
					return StructureStart.DEFAULT;
				});
			}
			map.entrySet().removeIf((Map.Entry<Structure, StructureStart> entry) -> !entry.getValue().hasChildren());
			if (!map.isEmpty()) {
				chunk.setStructureStarts(map);
			}
		}
	}

	public ChunkUngeneratedStructures getStructureStarts(StructureGenerationParams params) {
		ChunkUngeneratedStructures structures = this.worldUngeneratedStructures.get(params.chunkPos);
		if (structures == null) {
			this.worldUngeneratedStructures.put(
				params.chunkPos.toLong(),
				structures = this.computeStructureStarts(params)
			);
		}
		else {
			//System.out.println("Got cached structure starts at " + params.chunkPos);
		}
		return structures;
	}

	public @NotNull ChunkUngeneratedStructures computeStructureStarts(StructureGenerationParams params) {
		//System.out.println("Computing structure starts at " + params.chunkPos);
		Permuter structureChooser = new Permuter(0L);
		long chunkSeed = Permuter.permute(params.columnSeed() ^ 0x767DB826EDD5532EL, params.chunkPos.x, params.chunkPos.z);
		ChunkUngeneratedStructures toAdd = new ChunkUngeneratedStructures();
		for (RegistryEntry<StructureSet> structureSet : params.structurePlacementCalculator.getStructureSets()) {
			if (structureSet.value().placement().shouldGenerate(params.structurePlacementCalculator, params.chunkPos.x, params.chunkPos.z)) {
				structureChooser.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(structureSet).hashCode()));
				List<WeightedEntry> possibilities = new ArrayList<>(structureSet.value().structures());
				int totalWeight = getTotalWeight(possibilities);
				while (!possibilities.isEmpty()) {
					int index = getRandomIndex(possibilities, totalWeight, structureChooser);
					WeightedEntry entry = possibilities.get(index);
					SortedStructurePieces structure = this.computeStructureStart(params, entry);
					if (structure.getStart().hasChildren()) {
						toAdd.add(structure);
						break;
					}
					else {
						if (index == possibilities.size() - 1) {
							possibilities.remove(index);
						}
						else {
							possibilities.set(index, possibilities.get(possibilities.size() - 1));
							possibilities.remove(possibilities.size() - 1);
						}
						totalWeight -= entry.weight();
						continue;
					}
				}
			}
		}
		toAdd.sort(
			Comparator.comparingInt(
				SortedStructurePieces::volume
			)
		);
		toAdd.checkSelfIntersections();
		return toAdd;
	}

	public static int getTotalWeight(List<WeightedEntry> list) {
		int sum = 0;
		for (int index = 0, size = list.size(); index < size; index++) {
			sum += list.get(index).weight();
		}
		return sum;
	}

	public static int getRandomIndex(List<WeightedEntry> list, int totalWeight, RandomGenerator random) {
		int rng = random.nextInt(totalWeight);
		for (int index = 0, size = list.size(); index < size; index++) {
			if ((rng -= list.get(index).weight()) < 0) {
				return index;
			}
		}
		throw new IllegalStateException("either the RandomGenerator messed up, or the weights changed.");
	}

	public static String toString(StructureStart start) {
		return UnregisteredObjectException.getTagKey(start.getStructure().getValidBiomes()).id() + " @ " + start.getBoundingBox().getCenter();
	}

	public @NotNull SortedStructurePieces computeStructureStart(StructureGenerationParams params, WeightedEntry weightedEntry) {
		if (ValkyrienSkiesCompat.isInShipyard(params.chunkPos)) {
			return SortedStructurePieces.EMPTY;
		}
		//System.out.println("Computing " + UnregisteredObjectException.getID(weightedEntry.structure()) + " at " + params.chunkPos);
		Structure structure = weightedEntry.structure().value();
		Predicate<RegistryEntry<Biome>> predicate = structure.getValidBiomes()::contains;
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate.value();
		}
		StructurePosition newStartPosition = structure.getValidStructurePosition(
			params.toStructureContext(Predicates.alwaysTrue())
		)
		.orElse(null);
		if (newStartPosition == null) return SortedStructurePieces.EMPTY;
		StructurePiecesCollector collector = newStartPosition.generate();
		StructureStart newStart = new StructureStart(structure, params.chunkPos, 0, collector.toList());
		if (!newStart.hasChildren()) return SortedStructurePieces.EMPTY;
		int oldY = newStart.getBoundingBox().getMinY();
		if (
			!params.generator.canStructureSpawn(
				weightedEntry.structure(),
				newStart,
				new Permuter(
					Permuter.permute(
						params.columnSeed() ^ 0xD59E69D9AB0D41BAL,
						//String.hashCode() will be cached, which means faster permutation times.
						UnregisteredObjectException.getID(weightedEntry.structure()).hashCode(),
						params.chunkPos.x,
						params.chunkPos.z
					)
				),
				DistantHorizonsCompat.isOnDistantHorizonThread()
			)
		) {
			return SortedStructurePieces.EMPTY;
		}
		int newY = newStart.getBoundingBox().getMinY();
		if (
			!predicate.test(
				params.biomeSource().getBiome(
					newStartPosition.position().getX() >> 2,
					(newStartPosition.position().getY() + (newY - oldY)) >> 2,
					newStartPosition.position().getZ() >> 2,
					params.noiseConfig.getMultiNoiseSampler()
				)
			)
		) {
			return SortedStructurePieces.EMPTY;
		}
		//expand structure bounding boxes so that overriders
		//which depend on them being expanded work properly.
		((StructureStart_BoundingBoxSetter)(Object)(newStart)).bigglobe_setBoundingBox(
			newStart.getBoundingBox().expand(
				weightedEntry.structure().value().getTerrainAdaptation() == StructureTerrainAdaptation.NONE
				? 16
				: 4
			)
		);
		return SortedStructurePieces.create(newStart);
	}

	public static class WorldUngeneratedStructures
	extends Long2ObjectLinkedOpenHashMap<
		ChunkUngeneratedStructures
	> {

		public final long retainTime;

		public WorldUngeneratedStructures(long retainTime) {
			this.retainTime = retainTime;
		}

		public void purge() {
			long deadline = System.currentTimeMillis() - this.retainTime;
			while (!this.isEmpty()) {
				ChunkUngeneratedStructures value = this.firstValue();
				if (value.wasUsed(deadline)) break;
				else this.removeFirst();
			}
		}

		@SuppressWarnings("MethodOverloadsMethodOfSuperclass")
		public ChunkUngeneratedStructures get(ChunkPos pos) {
			return this.get(pos.x, pos.z);
		}

		public ChunkUngeneratedStructures get(int x, int z) {
			ChunkUngeneratedStructures value = this.getAndMoveToLast(ChunkPos.toLong(x, z));
			if (value != null) value.markUsed();
			this.purge();
			return value;
		}

		public ChunkUngeneratedStructures firstValue() {
			if (this.size == 0) throw new NoSuchElementException();
			return (ChunkUngeneratedStructures)(
				(
					(Object[])(this.value)
				)
				[this.first]
			);
		}
	}

	public static class ChunkUngeneratedStructures extends ObjectArrayList<SortedStructurePieces> {

		public long timestamp = System.currentTimeMillis();

		public ChunkUngeneratedStructures() {}

		@SuppressWarnings("CopyConstructorMissesField")
		public ChunkUngeneratedStructures(ChunkUngeneratedStructures other) {
			super(other);
		}

		public void markUsed() {
			this.timestamp = System.currentTimeMillis();
		}

		public boolean wasUsed(long deadline) {
			return this.timestamp >= deadline;
		}

		public void checkSelfIntersections() {
			int size = this.size;
			if (size <= 1) return;
			Object[] elements = this.elements();
			for (int smallerIndex = 0; smallerIndex < size; smallerIndex++) {
				SortedStructurePieces smallerStructure = (SortedStructurePieces)(elements[smallerIndex]);
				for (int largerIndex = smallerIndex; ++largerIndex < size;) {
					SortedStructurePieces largerStructure = (SortedStructurePieces)(elements[largerIndex]);
					if (SortedStructurePieces.intersects(smallerStructure, largerStructure)) {
						//System.out.println("Prevented self-intersection between " + StructureManager.toString(smallerStructure.getStart()) + " and " + StructureManager.toString(largerStructure.getStart()));
						elements[smallerIndex] = null;
						break;
					}
				}
			}
			this.removeNulls();
		}

		public void removeIntersecting(ChunkUngeneratedStructures other) {
			int size = this.size;
			if (size == 0) return;
			int otherSize = other.size();
			if (otherSize == 0) return;
			Object[] elements = this.elements();
			for (int smallerIndex = 0; smallerIndex < size; smallerIndex++) {
				SortedStructurePieces smallerStructure = (SortedStructurePieces)(elements[smallerIndex]);
				for (int largerIndex = otherSize; --largerIndex >= 0;) {
					SortedStructurePieces largerStructure = other.get(largerIndex);
					if (largerStructure.volume() >= smallerStructure.volume()) {
						if (SortedStructurePieces.intersects(smallerStructure, largerStructure)) {
							//System.out.println("Prevented intersection between " + StructureManager.toString(smallerStructure.getStart()) + " and " + StructureManager.toString(largerStructure.getStart()));
							elements[smallerIndex] = null;
							break;
						}
					}
					else {
						break;
					}
				}
			}
			this.removeNulls();
		}

		public void removeNulls() {
			Object[] elements = this.elements();
			int size = this.size;
			int writeIndex = 0;
			for (int readIndex = 0; readIndex < size; readIndex++) {
				if (elements[readIndex] != null) {
					elements[writeIndex++] = elements[readIndex];
				}
			}
			this.size = writeIndex;
			while (writeIndex < size) {
				elements[writeIndex++] = null;
			}
		}
	}

	public static interface SortedStructurePieces {

		public static final SortedStructurePieces EMPTY = create(StructureStart.DEFAULT);

		public static SortedStructurePieces create(StructureStart start) {
			assert start.hasChildren() || EMPTY == null;
			return new SectionSortedStructurePieces(start);
		}

		public abstract StructureStart getStart();

		public abstract boolean intersects(StructurePiece piece);

		public default boolean intersects(StructureStart start) {
			for (StructurePiece child : start.getChildren()) {
				if (this.intersects(child)) return true;
			}
			return false;
		}

		public static boolean intersects(SortedStructurePieces pieces1, SortedStructurePieces pieces2) {
			return pieces1.getStart().getChildren().size() < pieces2.getStart().getChildren().size() ? pieces2.intersects(pieces1.getStart()) : pieces1.intersects(pieces2.getStart());
		}

		public abstract int volume();

		public static int volumeOf(StructureStart start) {
			if (!start.hasChildren()) return 0;
			BlockBox box = start.getBoundingBox();
			int minX = box.getMinX() + 16;
			int minY = box.getMinY() + 16;
			int minZ = box.getMinZ() + 16;
			int maxX = box.getMaxX() - 16;
			int maxY = box.getMaxY() - 16;
			int maxZ = box.getMaxZ() - 16;
			assert maxX >= minX && maxY >= minY && maxZ >= minZ;
			return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		}
	}

	public static class ChunkSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> implements SortedStructurePieces {

		public final StructureStart start;
		public final int volume;

		public ChunkSortedStructurePieces(StructureStart start) {
			super(start.getChildren().size());
			this.start = start;
			this.volume = SortedStructurePieces.volumeOf(start);
			for (StructurePiece piece : start.getChildren()) {
				BlockBox box = piece.getBoundingBox();
				int minX = box.getMinX() >> 4;
				int minZ = box.getMinZ() >> 4;
				int maxX = box.getMaxX() >> 4;
				int maxZ = box.getMaxZ() >> 4;
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						this
						.computeIfAbsent(
							ChunkPos.toLong(x, z),
							(long pos) -> new ArrayList<>(8)
						)
						.add(piece);
					}
				}
			}
		}

		@Override
		public boolean intersects(StructurePiece piece) {
			BlockBox box = piece.getBoundingBox();
			int minX = box.getMinX() >> 4;
			int minZ = box.getMinZ() >> 4;
			int maxX = box.getMaxX() >> 4;
			int maxZ = box.getMaxZ() >> 4;
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					List<StructurePiece> list = this.get(ChunkPos.toLong(x, z));
					if (list != null) {
						for (int index = 0, size = list.size(); index < size; index++) {
							if (list.get(index).getBoundingBox().intersects(box)) return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public StructureStart getStart() {
			return this.start;
		}

		@Override
		public int volume() {
			return this.volume;
		}
	}

	public static class SectionSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> implements SortedStructurePieces {

		public final StructureStart start;
		public final int volume;

		public SectionSortedStructurePieces(StructureStart start) {
			super(start.getChildren().size());
			this.start = start;
			this.volume = SortedStructurePieces.volumeOf(start);
			for (StructurePiece piece : start.getChildren()) {
				BlockBox box = piece.getBoundingBox();
				int minX = box.getMinX() >> 4;
				int minY = box.getMinY() >> 4;
				int minZ = box.getMinZ() >> 4;
				int maxX = box.getMaxX() >> 4;
				int maxY = box.getMaxY() >> 4;
				int maxZ = box.getMaxZ() >> 4;
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						for (int y = minY; y <= maxY; y++) {
							this
							.computeIfAbsent(
								ChunkSectionPos.asLong(x, y, z),
								(long pos) -> new ArrayList<>(4)
							)
							.add(piece);
						}
					}
				}
			}
		}

		@Override
		public boolean intersects(StructurePiece piece) {
			BlockBox box = piece.getBoundingBox();
			int minX = box.getMinX() >> 4;
			int minY = box.getMinY() >> 4;
			int minZ = box.getMinZ() >> 4;
			int maxX = box.getMaxX() >> 4;
			int maxY = box.getMaxY() >> 4;
			int maxZ = box.getMaxZ() >> 4;
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						List<StructurePiece> list = this.get(ChunkSectionPos.asLong(x, y, z));
						if (list != null) {
							for (int index = 0, size = list.size(); index < size; index++) {
								if (list.get(index).getBoundingBox().intersects(box)) return true;
							}
						}
					}
				}
			}
			return false;
		}

		@Override
		public StructureStart getStart() {
			return this.start;
		}

		@Override
		public int volume() {
			return this.volume;
		}
	}
}