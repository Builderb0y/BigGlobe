package builderb0y.bigglobe.structures;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.*;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.ValkyrienSkiesCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;
import builderb0y.bigglobe.util.LinkedArrayList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public class StructureManager {

	public final UngeneratedStructures<StructureKey, SectionSortedStructurePieces>
		potentialStructures    = new UngeneratedStructures<>(60_000L);
	public final UngeneratedStructures<ChunkPos, FinalStructures>
		finalStructures        = new UngeneratedStructures<>(60_000L),
		intersectingStructures = new UngeneratedStructures<>(60_000L);

	public static record StructureGenerationParams(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
		Hints hints,
		StructurePlacementCalculator structurePlacementCalculator,
		DynamicRegistryManager dynamicRegistries,
		NoiseConfig noiseConfig,
		StructureTemplateManager structureTemplateManager,
		HeightLimitView heightLimitView,
		ChunkPos chunkPos,
		boolean distantHorizons
	) {

		public StructureGenerationParams(
			BigGlobeScriptedChunkGenerator generator,
			ScriptedColumnLookup columns,
			Hints hints,
			ServerWorld world,
			ChunkPos chunkPos,
			boolean distantHorizons
		) {
			this(
				generator,
				columns,
				hints,
				world.getChunkManager().getStructurePlacementCalculator(),
				world.getRegistryManager(),
				world.getChunkManager().getNoiseConfig(),
				world.getStructureTemplateManager(),
				world,
				chunkPos,
				distantHorizons
			);
		}

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
				this.columns,
				this.hints,
				this.structurePlacementCalculator,
				this.dynamicRegistries,
				this.noiseConfig,
				this.structureTemplateManager,
				this.heightLimitView,
				pos,
				this.distantHorizons
			);
		}
	}

	public static RegistryKey<Structure> structureKey(Structure structure) {
		return RegistryVersions.getRegistry(BigGlobeMod.getCurrentServer().getRegistryManager(), RegistryKeys.STRUCTURE).getKey(structure).orElseThrow();
	}

	public static Identifier structureID(Structure structure) {
		return structureKey(structure).getValue();
	}

	public static String structureName(Structure structure) {
		return structureID(structure).toString();
	}

	public static OptionalInt maxSize(RegistryEntry<StructureSet> set) {
		return (
			set
			.value()
			.structures()
			.stream()
			.map(WeightedEntry::structure)
			.map(RegistryEntry<Structure>::value)
			.map(SizedStructure.class::cast)
			.mapToInt(SizedStructure::bigglobe_getMaxRadiusInChunks)
			.max()
		);
	}

	public static Stream<StructureKey> getFilteredStartChunks(StructureGenerationParams params, RegistryEntry<StructureSet> set, int radius) {
		return (
			(StreamableStructurePlacement)(
				set.value().placement()
			)
		)
		.bigglobe_getFilteredStartChunks(
			params.generator,
			params.structurePlacementCalculator,
			params.chunkPos.x,
			params.chunkPos.z,
			radius
		)
		.map((ChunkPos chunkPos) -> new StructureKey(chunkPos.x, chunkPos.z, set));
	}

	public FinalStructures getIntersectingStructures(StructureGenerationParams params) {
		FinalStructures starts;
		synchronized (this.intersectingStructures) {
			starts = this.intersectingStructures.get(params.chunkPos);
		}
		if (starts != null) return starts;
		starts = this.computeIntersectingStructures(params);
		synchronized (this.intersectingStructures) {
			FinalStructures existing = this.intersectingStructures.putIfAbsent(params.chunkPos, starts);
			if (existing != null) starts = existing;
		}
		return starts;
	}

	public FinalStructures computeIntersectingStructures(StructureGenerationParams params) {
		return (
			params
			.structurePlacementCalculator
			.getStructureSets()
			.stream()
			.flatMap((RegistryEntry<StructureSet> set) -> {
				return maxSize(set).stream().mapToObj((int radius) -> getFilteredStartChunks(params, set, radius)).flatMap(Function.identity());
			})
			.flatMap((StructureKey key) -> (
				this
				.getStructureStarts(params.at(key.chunkX, key.chunkZ))
				.stream()
				.filter((StructureStart start) -> (
					key
					.set
					.value()
					.structures()
					.stream()
					.map(WeightedEntry::structure)
					.map(RegistryEntry<Structure>::value)
					.anyMatch(start.getStructure()::equals)
				))
			))
			.filter((StructureStart start) -> (
				params.chunkPos.x >= start.getBoundingBox().getMinX() >> 4 &&
				params.chunkPos.z >= start.getBoundingBox().getMinZ() >> 4 &&
				params.chunkPos.x <= start.getBoundingBox().getMaxX() >> 4 &&
				params.chunkPos.z <= start.getBoundingBox().getMaxZ() >> 4
			))
			.collect(Collectors.toCollection(FinalStructures::new))
		);
	}

	public FinalStructures getStructureStarts(StructureGenerationParams params) {
		FinalStructures finalStructures;
		synchronized (this.finalStructures) {
			finalStructures = this.finalStructures.get(params.chunkPos);
		}
		if (finalStructures != null) return finalStructures;
		finalStructures = this.computeStructureStarts(params);
		synchronized (this.finalStructures) {
			FinalStructures existing = this.finalStructures.putIfAbsent(params.chunkPos, finalStructures);
			if (existing != null) finalStructures = existing;
		}
		return finalStructures;
	}

	public FinalStructures computeStructureStarts(StructureGenerationParams params) {
		LinkedArrayList<SectionSortedStructurePieces> starts = new LinkedArrayList<>();
		MutableInt maxSizeForChunk = new MutableInt(0);

		//step 1: get all the structures that start in the current chunk.
		//also compute the size of the largest one.
		params
		.structurePlacementCalculator
		.getStructureSets()
		.stream()
		.flatMap((RegistryEntry<StructureSet> set) -> {
			return getFilteredStartChunks(params, set, 0);
		})
		.forEach((StructureKey key) -> {
			SectionSortedStructurePieces pieces = this.getStructureStart(params.at(key.chunkX, key.chunkZ), key.set);
			if (!pieces.isEmpty()) {
				starts.addElementToEnd(pieces);
				maxSize(key.set).ifPresent((int size) -> maxSizeForChunk.setValue(Math.max(maxSizeForChunk.getValue(), size)));
			}
		});
		if (starts.isEmpty()) return new FinalStructures(0);

		//step 2: get all the structures which could potentially collide with anything in the current chunk.
		params
		.structurePlacementCalculator
		.getStructureSets()
		.stream()
		.flatMap((RegistryEntry<StructureSet> set) -> {
			return maxSize(set).stream().mapToObj((int radius) -> {
				return getFilteredStartChunks(params, set, maxSizeForChunk.getValue() + radius);
			})
			.flatMap(Function.identity());
		})
		.filter((StructureKey key) -> key.chunkX != params.chunkPos.x || key.chunkZ != params.chunkPos.z)
		.forEach((StructureKey key) -> {
			SectionSortedStructurePieces pieces = this.getStructureStart(params.at(key.chunkX, key.chunkZ), key.set);
			if (!pieces.isEmpty()) {
				starts.addElementToEnd(pieces);
			}
		});

		//step 3: filter, log, and convert.
		LinkedArrayList<SectionSortedStructurePieces> filtered = removeIntersections(params, starts);
		if (filtered.isEmpty()) return new FinalStructures(0);
		boolean log = BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning;
		FinalStructures result = new FinalStructures(filtered.size());
		while (!filtered.isEmpty()) {
			StructureStart start = filtered.removeFirstElement().startWrapper.start();
			if (log) BigGlobeMod.LOGGER.info("Structure " + structureName(start.getStructure()) + " spawned at " + start.getBoundingBox().getCenter());
			result.add(start);
		}
		return result;
	}

	public static LinkedArrayList<SectionSortedStructurePieces> removeIntersections(StructureGenerationParams params, LinkedArrayList<SectionSortedStructurePieces> list) {
		SortedOverriders overriders = params.generator.getOverriders();
		LinkedArrayList<SectionSortedStructurePieces> filtered = new LinkedArrayList<>();
		outer:
		for (LinkedArrayList.Node<SectionSortedStructurePieces> current = list.first; current != null; current = current.next) {
			if (current.element.startWrapper.start().getPos().equals(params.chunkPos)) {
				for (LinkedArrayList.Node<SectionSortedStructurePieces> other = list.first; other != null; other = other.next) {
					if (other == current) continue;
					if (SectionSortedStructurePieces.intersects(current.element, other.element)) {
						int priority = overriders.getCollisionPriority(params.columns, current.element.startWrapper, other.element.startWrapper, params.hints);
						if (priority < 0) {
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + current.element + " did not spawn because it collided with a " + other.element + " and a collision overrider returned a negative priority.");
							}
							continue outer;
						}
						else if (priority == 0 && current.element.volume <= other.element.volume) {
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + current.element + " did not spawn because it collided with a " + other.element + " and the other structure is bigger.");
							}
							continue outer;
						}
					}
				}
				filtered.addElementToEnd(current.element);
			}
		}
		return filtered;
	}

	public @NotNull SectionSortedStructurePieces getStructureStart(StructureGenerationParams params, RegistryEntry<StructureSet> set) {
		SectionSortedStructurePieces pieces;
		StructureKey key = new StructureKey(params.chunkPos.x, params.chunkPos.z, set);
		synchronized (this.potentialStructures) {
			pieces = this.potentialStructures.get(key);
		}
		if (pieces == null) {
			StructureStartWrapper wrapper = this.computeStructureStart(params, set);
			if (wrapper != null) {
				pieces = new SectionSortedStructurePieces(set, wrapper);
			}
			else {
				pieces = new SectionSortedStructurePieces(set);
			}
			synchronized (this.potentialStructures) {
				SectionSortedStructurePieces existing = this.potentialStructures.putIfAbsent(key, pieces);
				if (existing != null) pieces = existing;
			}
		}
		return pieces;
	}

	public @Nullable StructureStartWrapper computeStructureStart(StructureGenerationParams params, RegistryEntry<StructureSet> set) {
		Permuter structureChooser = new Permuter(0L);
		long chunkSeed = Permuter.permute(params.columnSeed() ^ 0x767DB826EDD5532EL, params.chunkPos.x, params.chunkPos.z);
		structureChooser.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(set).hashCode()));
		List<WeightedEntry> possibilities = new ArrayList<>(set.value().structures());
		int totalWeight = getTotalWeight(possibilities);
		while (!possibilities.isEmpty()) {
			int index = getRandomIndex(possibilities, totalWeight, structureChooser);
			WeightedEntry entry = possibilities.get(index);
			StructureStartWrapper structure = this.computeStructureStart(params, entry);
			if (structure != null && structure.start().hasChildren()) {
				return structure;
			}
			else {
				if (index == possibilities.size() - 1) {
					possibilities.remove(index);
				}
				else {
					possibilities.set(index, possibilities.remove(possibilities.size() - 1));
				}
				totalWeight -= entry.weight();
			}
		}
		return null;
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

	public @Nullable StructureStartWrapper computeStructureStart(StructureGenerationParams params, WeightedEntry weightedEntry) {
		if (ValkyrienSkiesCompat.isInShipyard(params.chunkPos)) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos + " because Valkyrien Skies reserves this area.");
			}
			return null;
		}
		Structure structure = weightedEntry.structure().value();
		RegistryEntryList<Biome> validBiomes = structure.getValidBiomes();
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate().value();
		}
		StructurePosition newStartPosition = structure.getValidStructurePosition(
			params.toStructureContext(Predicates.alwaysTrue())
		)
		.orElse(null);
		if (newStartPosition == null) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos + " because the structure itself decided not to.");
			}
			return null;
		}
		StructurePiecesCollector collector = newStartPosition.generate();
		StructureStart newStart = new StructureStart(structure, params.chunkPos, 0, collector.toList());
		if (!newStart.hasChildren()) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos + " because the resulting structure has no pieces.");
			}
			return null;
		}
		StructureStartWrapper wrapper = StructureStartWrapper.of(weightedEntry.structure(), newStart);
		int oldY = newStart.getBoundingBox().getMinY();
		if (
			!this.canStructureSpawn(
				params,
				wrapper,
				newStart,
				new Permuter(
					Permuter.permute(
						params.columnSeed() ^ 0xD59E69D9AB0D41BAL,
						//String.hashCode() will be cached, which means faster permutation times.
						UnregisteredObjectException.getID(weightedEntry.structure()).hashCode(),
						params.chunkPos.x,
						params.chunkPos.z
					)
				)
			)
		) {
			//canStructureSpawn() logs a message on its own.
			return null;
		}
		int newY = newStart.getBoundingBox().getMinY();
		if (
			!validBiomes.contains(
				params.biomeSource().getBiome(
					newStartPosition.position().getX() >> 2,
					(newStartPosition.position().getY() + (newY - oldY)) >> 2,
					newStartPosition.position().getZ() >> 2,
					params.noiseConfig.getMultiNoiseSampler()
				)
			)
		) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn at " + newStart.getBoundingBox().getCenter() + " because the biome at this location is not in the structure's biome tag.");
			}
			return null;
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
		return wrapper;
	}

	public boolean canStructureSpawn(
		StructureGenerationParams params,
		StructureStartWrapper wrapper,
		StructureStart start,
		Permuter permuter
	) {
		Hints hints = ColumnUsage.GENERIC.maybeDhHints(params.distantHorizons);
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(
			params.generator.columnEntryRegistry.columnFactory,
			new Params(
				params.generator, 0, 0, hints
			)
		);
		for (StructureOverrider.Entry overrider : params.generator.getOverriders().structures) {
			if (!overrider.script().override(lookup, wrapper, permuter, params.generator.columnSeed, hints)) {
				if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
					BigGlobeMod.LOGGER.info(
						"Structure " +
						structureName(start.getStructure()) +
						" did not spawn at " +
						start.getBoundingBox().getCenter() +
						" because overrider " +
						RegistryVersions.getRegistry(
							BigGlobeMod.getCurrentServer().getRegistryManager(),
							BigGlobeDynamicRegistries.OVERRIDER_REGISTRY_KEY
						)
						.getId(overrider) +
						" said no."
					);
				}
				return false;
			}
		}
		return true;
	}

	public static class UngeneratedStructures<K, V extends Timestamped> extends Object2ObjectLinkedOpenHashMap<K, V> {

		public final long retainTimeMilliseconds;

		public UngeneratedStructures(long retainTimeMilliseconds) {
			super(128);
			this.retainTimeMilliseconds = retainTimeMilliseconds;
		}

		@Override
		@SuppressWarnings("unchecked")
		public V get(Object key) {
			V pieces = this.getAndMoveToLast((K)(key));
			if (pieces != null) pieces.markUsed();
			this.purge();
			return pieces;
		}

		public void purge() {
			long deadline = System.currentTimeMillis() - this.retainTimeMilliseconds;
			while (!this.isEmpty()) {
				V value = this.firstValue();
				if (value.wasUsed(deadline)) break;
				else this.removeFirst();
			}
		}

		@SuppressWarnings("unchecked")
		public V firstValue() {
			if (this.size == 0) throw new NoSuchElementException();
			return (V)(
				(
					(Object[])(this.value)
				)
				[this.first]
			);
		}
	}

	public static record StructureKey(int chunkX, int chunkZ, RegistryEntry<StructureSet> set) {}

	public static interface Timestamped {

		public abstract long getTimestamp();

		public abstract void setTimestamp(long timestamp);

		public default void markUsed() {
			this.setTimestamp(System.currentTimeMillis());
		}

		public default boolean wasUsed(long deadline) {
			return this.getTimestamp() >= deadline;
		}
	}

	/*
	public static class BvhSortedStructurePieces implements Timestamped {

		public final RegistryEntry<StructureSet> set;
		public final StructureStartWrapper startWrapper;
		public final int volume;
		public final Node root;
		public long timestamp; @Override public long getTimestamp() { return this.timestamp; } @Override public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

		public BvhSortedStructurePieces(RegistryEntry<StructureSet> set, StructureStartWrapper startWrapper) {
			this.set = set;
			this.startWrapper = startWrapper;
			this.volume = volumeOf(startWrapper.start());
			List<StructurePiece> children = startWrapper.start().getChildren();
			int size = children.size();
			BlockBox[] boxes = new BlockBox[size];
			for (int index = 0; index < size; index++) {
				boxes[index] = children.get(index).getBoundingBox();
			}
			this.root = buildRoot(boxes, 0, size);
		}

		public BvhSortedStructurePieces(RegistryEntry<StructureSet> set) {
			this.set = set;
			this.startWrapper = null;
			this.volume = 0;
			this.root = EmptyNode.INSTANCE;
		}

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

		public static Node buildRoot(BlockBox[] boxes, int startIndex, int endIndex) {
			int size = endIndex - startIndex;
			return switch (size) {
				case 0 -> EmptyNode.INSTANCE;
				case 1 -> new SingleNode(boxes[startIndex]);
				case 2 -> new TwoNode(
					new SingleNode(boxes[startIndex]),
					new SingleNode(boxes[startIndex + 1]),
					WorldUtil.union(boxes[startIndex], boxes[startIndex + 1])
				);
				default -> {
					int
						minX = Integer.MAX_VALUE,
						minY = Integer.MAX_VALUE,
						minZ = Integer.MAX_VALUE,
						maxX = Integer.MIN_VALUE,
						maxY = Integer.MIN_VALUE,
						maxZ = Integer.MIN_VALUE;
					for (int index = startIndex; index < endIndex; index++) {
						BlockBox box = boxes[index];
						minX = Math.min(minX, box.getMinX());
						minY = Math.min(minY, box.getMinY());
						minZ = Math.min(minZ, box.getMinZ());
						maxX = Math.max(maxX, box.getMaxX());
						maxY = Math.max(maxY, box.getMaxY());
						maxZ = Math.max(maxZ, box.getMaxZ());
					}
					int sizeX = maxX - minX;
					int sizeY = maxY - minY;
					int sizeZ = maxZ - minZ;
					if (sizeX >= sizeY && sizeX >= sizeZ) {
						Arrays.sort(boxes, startIndex, endIndex, Comparator.comparingInt((BlockBox box) -> box.getMinX() + box.getMaxX()));
					}
					else if (sizeZ >= sizeY && sizeZ >= sizeX) {
						Arrays.sort(boxes, startIndex, endIndex, Comparator.comparingInt((BlockBox box) -> box.getMinZ() + box.getMaxZ()));
					}
					else {
						Arrays.sort(boxes, startIndex, endIndex, Comparator.comparingInt((BlockBox box) -> box.getMinY() + box.getMaxY()));
					}
					int threshold = (startIndex + endIndex) >> 1;
					yield new TwoNode(
						buildRoot(boxes, startIndex, threshold),
						buildRoot(boxes, threshold, endIndex),
						new BlockBox(minX, minY, minZ, maxX, maxY, maxZ)
					);
				}
			};
		}

		public static boolean intersects(BvhSortedStructurePieces pieces1, BvhSortedStructurePieces pieces2) {
			StructureStart start1 = pieces1.startWrapper.start();
			StructureStart start2 = pieces2.startWrapper.start();
			return start1.getChildren().size() < start2.getChildren().size() ? pieces1.intersects(pieces2) : pieces2.intersects(pieces1);
		}

		public boolean isEmpty() {
			return this.root == EmptyNode.INSTANCE;
		}

		public boolean intersects(BlockBox box) {
			return this.root.intersects(box);
		}

		public boolean intersects(BvhSortedStructurePieces pieces) {
			return this.root.intersects(pieces.root);
		}

		@Override
		public String toString() {
			return UnregisteredObjectException.getID(this.startWrapper.entry().entry) + " at " + this.startWrapper.start().getBoundingBox();
		}

		public static abstract class Node {

			public final BlockBox bounds;
			public final byte type;

			public Node(BlockBox bounds, int type) {
				this.bounds = bounds;
				this.type = (byte)(type);
			}

			public abstract boolean intersects(BlockBox box);

			public abstract boolean intersects(Node node);
		}

		public static class EmptyNode extends Node {

			public static final EmptyNode INSTANCE = new EmptyNode();

			public EmptyNode() {
				super(new BlockBox(0, 0, 0, 0, 0, 0), 0);
			}

			@Override
			public boolean intersects(BlockBox box) {
				return false;
			}

			@Override
			public boolean intersects(Node node) {
				return false;
			}
		}

		public static class SingleNode extends Node {

			public SingleNode(BlockBox box) {
				super(box, 1);
			}

			@Override
			public boolean intersects(BlockBox box) {
				return this.bounds.intersects(box);
			}

			@Override
			public boolean intersects(Node node) {
				return node.intersects(this.bounds);
			}
		}

		public static class TwoNode extends Node {

			public final Node a, b;

			public TwoNode(Node a, Node b, BlockBox union) {
				super(union, 2);
				this.a = a;
				this.b = b;
			}

			@Override
			public boolean intersects(BlockBox box) {
				return this.bounds.intersects(box) && (this.a.intersects(box) || this.b.intersects(box));
			}

			@Override
			public boolean intersects(Node node) {
				if (this.bounds.intersects(node.bounds)) {
					return switch (node.type) {
						case 0 -> false;
						case 1 -> this.a.intersects(node.bounds) || this.b.intersects(node.bounds);
						default -> {
							TwoNode that = (TwoNode)(node);
							yield this.a.intersects(that.a) || this.a.intersects(that.b) || this.b.intersects(that.a) || this.b.intersects(that.b);
						}
					};
				}
				return false;
			}
		}
	}
	*/

	public static class SectionSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> implements Timestamped {

		public final RegistryEntry<StructureSet> set;
		public final StructureStartWrapper startWrapper;
		public final int volume, scale;
		public long timestamp; @Override public long getTimestamp() { return this.timestamp; } @Override public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

		public SectionSortedStructurePieces(RegistryEntry<StructureSet> set) {
			this.set = set;
			this.startWrapper = null;
			this.volume = 0;
			this.scale = 0;
			this.markUsed();
		}

		public SectionSortedStructurePieces(RegistryEntry<StructureSet> set, StructureStartWrapper startWrapper) {
			super(startWrapper.start().getChildren().size());
			this.set = set;
			this.startWrapper = startWrapper;
			this.volume = volumeOf(startWrapper.start());
			List<StructurePiece> children = startWrapper.start().getChildren();
			this.scale = Math.getExponent(Math.cbrt(((double)(this.volume)) / ((double)(children.size()))));
			for (StructurePiece piece : children) {
				BlockBox box = piece.getBoundingBox();
				int minX = box.getMinX() >> this.scale;
				int minY = box.getMinY() >> this.scale;
				int minZ = box.getMinZ() >> this.scale;
				int maxX = box.getMaxX() >> this.scale;
				int maxY = box.getMaxY() >> this.scale;
				int maxZ = box.getMaxZ() >> this.scale;
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						for (int y = minY; y <= maxY; y++) {
							this
							.computeIfAbsent(
								BlockPos.asLong(x, y, z),
								(long pos) -> new ArrayList<>(4)
							)
							.add(piece);
						}
					}
				}
			}
			this.markUsed();
		}

		public boolean intersects(StructureStart start) {
			for (StructurePiece child : start.getChildren()) {
				if (this.intersects(child)) return true;
			}
			return false;
		}

		public static boolean intersects(SectionSortedStructurePieces pieces1, SectionSortedStructurePieces pieces2) {
			StructureStart start1 = pieces1.startWrapper.start();
			StructureStart start2 = pieces2.startWrapper.start();
			return start1.getChildren().size() < start2.getChildren().size() ? pieces2.intersects(start1) : pieces1.intersects(start2);
		}

		public static int volumeOf(StructureStart start) {
			if (!start.hasChildren()) return 0;
			BlockBox box = start.getBoundingBox();
			int minX = box.getMinX();
			int minY = box.getMinY();
			int minZ = box.getMinZ();
			int maxX = box.getMaxX();
			int maxY = box.getMaxY();
			int maxZ = box.getMaxZ();
			return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		}

		public boolean intersects(StructurePiece piece) {
			BlockBox box = piece.getBoundingBox();
			int minX = box.getMinX() >> this.scale;
			int minY = box.getMinY() >> this.scale;
			int minZ = box.getMinZ() >> this.scale;
			int maxX = box.getMaxX() >> this.scale;
			int maxY = box.getMaxY() >> this.scale;
			int maxZ = box.getMaxZ() >> this.scale;
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						List<StructurePiece> list = this.get(BlockPos.asLong(x, y, z));
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
		public String toString() {
			return UnregisteredObjectException.getID(this.startWrapper.entry().entry) + " at " + this.startWrapper.start().getBoundingBox();
		}
	}

	public static class FinalStructures extends ObjectArrayList<StructureStart> implements Timestamped {

		public long timestamp; @Override public long getTimestamp() { return this.timestamp; } @Override public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

		public FinalStructures() {
			this.markUsed();
		}

		public FinalStructures(int capacity) {
			super(capacity);
			this.markUsed();
		}
	}
}