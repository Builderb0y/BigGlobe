package builderb0y.bigglobe.structures;

import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.*;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
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
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.ValkyrienSkiesCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.mixins.StructureStart_BoundingBoxSetter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public class StructureManager {

	public static final boolean DEBUG_REMOVED = false;
	public static final List<PotentialStructure> POTENTIAL_STRUCTURES = DEBUG_REMOVED ? new ArrayList<>() : null;

	public final WorldUngeneratedStructures worldUngeneratedStructures = new WorldUngeneratedStructures(60_000);

	public StructureManager() {
		if (DEBUG_REMOVED) POTENTIAL_STRUCTURES.clear();
	}

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

	public static void addPotentialStructure(StructureStart start, String failureReason) {
		/*
		if (structureID(start.getStructure()).getPath().contains("mega_tree")) {
			POTENTIAL_STRUCTURES.add(new PotentialStructure(start, failureReason));
		}
		//*/
	}

	public void setStructureStarts(StructureGenerationParams params, Chunk chunk) {
		if (!chunk.getStructureStarts().isEmpty()) {
			BigGlobeMod.LOGGER.warn(chunk + " already has structure starts");
			return;
		}
		ChunkUngeneratedStructures toAdd = this.getStructureStarts(params);
		outer:
		if (!toAdd.isEmpty()) {
			toAdd = new ChunkUngeneratedStructures(toAdd);
			for (int offsetZ = -16; offsetZ <= 16; offsetZ++) {
				for (int offsetX = -16; offsetX <= 16; offsetX++) {
					if (offsetX == 0 && offsetZ == 0) continue;
					StructureGenerationParams params2 = params.at(
						params.chunkPos.x + offsetX,
						params.chunkPos.z + offsetZ
					);
					toAdd.removeIntersecting(this.getStructureStarts(params2), params);
					if (toAdd.isEmpty()) break outer;
				}
			}
			Map<Structure, StructureStart> map = new HashMap<>(toAdd.size());
			for (SortedStructurePieces pieces : toAdd) {
				if (DEBUG_REMOVED) {
					addPotentialStructure(pieces.getStart(), null);
				}
				//System.out.println("Survivor: " + toString(pieces.getStart()));
				map.merge(pieces.getStart().getStructure(), pieces.getStart(), (StructureStart start1, StructureStart start2) -> {
					if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
						BigGlobeMod.LOGGER.info("More than one copy of structure " + structureName(start1.getStructure()) + " started in the same chunk. It may be present in more than one structure set.");
					}
					return new StructureStart(
						start1.getStructure(),
						start1.getPos(),
						0,
						new StructurePiecesList(
							Stream
							.concat(
								start1.getChildren().stream(),
								start2.getChildren().stream()
							)
							.toList()
						)
					);
				});
			}
			if (!map.isEmpty()) {
				if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
					for (StructureStart start : map.values()) {
						BigGlobeMod.LOGGER.info("Structure " + structureName(start.getStructure()) + " spawned at " + start.getBoundingBox().getCenter());
					}
				}
				chunk.setStructureStarts(map);
			}
		}
	}

	public ChunkUngeneratedStructures getStructureStarts(StructureGenerationParams params) {
		ChunkUngeneratedStructures structures;
		synchronized (this.worldUngeneratedStructures) {
			structures = this.worldUngeneratedStructures.get(params.chunkPos);
		}
		if (structures == null) {
			structures = this.computeStructureStarts(params);
			synchronized (this.worldUngeneratedStructures) {
				this.worldUngeneratedStructures.put(params.chunkPos.toLong(), structures);
			}
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
					if (structure.hasChildren()) {
						toAdd.add(structure);
						break;
					}
					else {
						if (index == possibilities.size() - 1) {
							possibilities.remove(index);
						}
						else {
							possibilities.set(index, possibilities.remove(possibilities.size() - 1));
						}
						totalWeight -= entry.weight();
						continue;
					}
				}
			}
		}
		if (!toAdd.isEmpty()) {
			toAdd.checkSelfIntersections(params);
		}
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
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos + " because Valkyrien Skies reserves this area.");
			}
			return EmptySortedStructurePieces.INSTANCE;
		}
		//System.out.println("Computing " + UnregisteredObjectException.getID(weightedEntry.structure()) + " at " + params.chunkPos);
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
			return EmptySortedStructurePieces.INSTANCE;
		}
		StructurePiecesCollector collector = newStartPosition.generate();
		StructureStart newStart = new StructureStart(structure, params.chunkPos, 0, collector.toList());
		if (!newStart.hasChildren()) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos + " because the resulting structure has no pieces.");
			}
			return EmptySortedStructurePieces.INSTANCE;
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
			//canStructureSpawn() prints a message on its own.
			return EmptySortedStructurePieces.INSTANCE;
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
			if (DEBUG_REMOVED) {
				addPotentialStructure(newStart, "Incorrect biome");
			}
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn at " + newStart.getBoundingBox().getCenter() + " because the biome at this location is not in the structure's biome tag.");
			}
			return EmptySortedStructurePieces.INSTANCE;
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
		return SortedStructurePieces.create(wrapper);
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
			new ScriptedColumn.Params(
				params.generator, 0, 0, hints
			)
		);
		for (StructureOverrider.Entry overrider : params.generator.getOverriders().structures) {
			if (!overrider.script().override(lookup, wrapper, permuter, params.generator.columnSeed, hints)) {
				if (DEBUG_REMOVED) {
					StructureManager.addPotentialStructure(
						start,
						"overrider " +
						RegistryVersions.getRegistry(
							BigGlobeMod.getCurrentServer().getRegistryManager(),
							BigGlobeDynamicRegistries.OVERRIDER_REGISTRY_KEY
						)
						.getId(overrider) +
						" said no."
					);
				}
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

		public void checkSelfIntersections(StructureGenerationParams params) {
			int size = this.size;
			if (size <= 1) return;
			Object[] elements = this.elements();
			for (int currentIndex = 0; currentIndex < size; currentIndex++) {
				SortedStructurePieces currentStructure = (SortedStructurePieces)(elements[currentIndex]);
				for (int otherIndex = 0; otherIndex < size; otherIndex++) {
					if (otherIndex == currentIndex) continue;
					SortedStructurePieces otherStructure = (SortedStructurePieces)(elements[otherIndex]);
					if (otherStructure != null && SortedStructurePieces.intersects(currentStructure, otherStructure)) {
						int priority = params.generator.getOverriders().getCollisionPriority(params.columns, currentStructure, otherStructure, params.hints);
						if (priority < 0) {
							if (DEBUG_REMOVED) {
								addPotentialStructure(currentStructure.getStart(), "Self collision priority < 0");
							}
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + structureName(currentStructure.getStart().getStructure()) + " did not spawn at " + currentStructure.getStart().getBoundingBox().getCenter() + " because it collided with a " + structureName(otherStructure.getStart().getStructure()) + " in the same chunk and a collision overrider returned a negative priority.");
							}
							elements[currentIndex] = null;
						}
						else if (priority == 0 && currentStructure.volume() <= otherStructure.volume()) {
							if (DEBUG_REMOVED) {
								addPotentialStructure(currentStructure.getStart(), "Self collision with larger structure " + structureName(otherStructure.getStart().getStructure()) + " (priority 0)");
							}
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + structureName(currentStructure.getStart().getStructure()) + " did not spawn at " + currentStructure.getStart().getBoundingBox().getCenter() + " because it collided with a " + structureName(otherStructure.getStart().getStructure()) + " in the same chunk and the other structure is bigger.");
							}
							elements[currentIndex] = null;
						}
					}
				}
			}
			this.removeNulls();
		}

		public void removeIntersecting(
			ChunkUngeneratedStructures other,
			StructureGenerationParams params
		) {
			int size = this.size;
			if (size == 0) return;
			int otherSize = other.size();
			if (otherSize == 0) return;
			Object[] elements = this.elements();
			SortedOverriders overriders = params.generator.getOverriders();
			for (int currentIndex = 0; currentIndex < size; currentIndex++) {
				SortedStructurePieces currentStructure = (SortedStructurePieces)(elements[currentIndex]);
				for (int otherIndex = 0; otherIndex < otherSize; otherIndex++) {
					SortedStructurePieces otherStructure = other.get(otherIndex);
					if (SortedStructurePieces.intersects(currentStructure, otherStructure)) {
						int priority = overriders.getCollisionPriority(params.columns, currentStructure, otherStructure, params.hints);
						if (priority < 0) {
							if (DEBUG_REMOVED) {
								addPotentialStructure(currentStructure.getStart(), "Other collision priority < 0");
							}
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + structureName(currentStructure.getStart().getStructure()) + " did not spawn at " + currentStructure.getStart().getBoundingBox().getCenter() + " because it collided with a " + structureName(otherStructure.getStart().getStructure()) + " in a different chunk and a collision overrider returned a negative priority.");
							}
							elements[currentIndex] = null;
						}
						else if (priority == 0 && currentStructure.volume() <= otherStructure.volume()) {
							if (DEBUG_REMOVED) {
								addPotentialStructure(currentStructure.getStart(), "Other collision with larger structure " + structureName(otherStructure.getStart().getStructure()) + " (priority 0)");
							}
							if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
								BigGlobeMod.LOGGER.info("Structure " + structureName(currentStructure.getStart().getStructure()) + " did not spawn at " + currentStructure.getStart().getBoundingBox().getCenter() + " because it collided with a " + structureName(otherStructure.getStart().getStructure()) + " in a different chunk and the other structure is bigger.");
							}
							elements[currentIndex] = null;
						}
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

		public static SortedStructurePieces create(StructureStartWrapper wrapper) {
			assert wrapper.start().hasChildren();
			return new SectionSortedStructurePieces(wrapper);
		}

		public abstract StructureStartWrapper getWrapper();

		public default StructureStart getStart() {
			return this.getWrapper().start();
		}

		public default boolean hasChildren() {
			return this.getStart().hasChildren();
		}

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

		public default String defaultToString() {
			return structureName(this.getStart().getStructure()) + " at " + this.getStart().getBoundingBox();
		}
	}

	public static enum EmptySortedStructurePieces implements SortedStructurePieces {
		INSTANCE;

		@Override
		public StructureStartWrapper getWrapper() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean intersects(StructurePiece piece) {
			return false;
		}

		@Override
		public int volume() {
			return 0;
		}

		@Override
		public boolean hasChildren() {
			return false;
		}
	}

	public static class ChunkSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> implements SortedStructurePieces {

		public final StructureStartWrapper wrapper;
		@Override public StructureStartWrapper getWrapper() { return this.wrapper; }

		public final int volume;
		@Override public int volume() { return this.volume; }

		public ChunkSortedStructurePieces(StructureStartWrapper wrapper) {
			super(wrapper.start().getChildren().size());
			this.wrapper = wrapper;
			this.volume = SortedStructurePieces.volumeOf(wrapper.start());
			for (StructurePiece piece : wrapper.start().getChildren()) {
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
		public String toString() {
			return this.defaultToString();
		}
	}

	public static class SectionSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> implements SortedStructurePieces {

		public final StructureStartWrapper wrapper;
		@Override public StructureStartWrapper getWrapper() { return this.wrapper; }

		public final int volume;
		@Override public int volume() { return this.volume; }

		public SectionSortedStructurePieces(StructureStartWrapper wrapper) {
			super(wrapper.start().getChildren().size());
			this.wrapper = wrapper;
			this.volume = SortedStructurePieces.volumeOf(wrapper.start());
			for (StructurePiece piece : wrapper.start().getChildren()) {
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
		public String toString() {
			return this.defaultToString();
		}
	}

	public static class PotentialStructure {

		public final StructureStart start;
		public final @Nullable String failureReason;
		public final float brightness;

		public PotentialStructure(StructureStart start, @Nullable String failureReason) {
			this.start = start;
			this.failureReason = failureReason;
			this.brightness = (float)(Math.random() * 0.5D + 0.5D);
		}

		public void render(
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			double cameraX,
			double cameraY,
			double cameraZ
		) {
			BlockBox box = this.start.getBoundingBox();
			this.drawBox(matrices, vertexConsumers, box, cameraX, cameraY, cameraZ);
			//for (StructurePiece child : this.start.getChildren()) {
			//	this.drawBox(matrices, vertexConsumers, child.getBoundingBox());
			//}
			if (this.failureReason != null) {
				DebugRenderer.drawString(
					matrices,
					vertexConsumers,
					structureName(this.start.getStructure()),
					(box.getMinX() + box.getMaxX()) * 0.5D,
					box.getMaxY() + 3.0D,
					(box.getMinZ() + box.getMaxZ()) * 0.5D,
					-1
				);
				DebugRenderer.drawString(
					matrices,
					vertexConsumers,
					this.failureReason,
					(box.getMinX() + box.getMaxX()) * 0.5D,
					box.getMaxY() + 2.0D,
					(box.getMinZ() + box.getMaxZ()) * 0.5D,
					-1
				);
			}
		}

		public void drawBox(
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			BlockBox box,
			double cameraX,
			double cameraY,
			double cameraZ
		) {
			DebugRenderer.drawBox(
				matrices,
				vertexConsumers,
				Box.from(box).offset(-cameraX, -cameraY, -cameraZ),
				this.failureReason != null ? this.brightness : 0.0F,
				this.failureReason == null ? this.brightness : 0.0F,
				0.0F,
				0.25F
			);
		}
	}
}