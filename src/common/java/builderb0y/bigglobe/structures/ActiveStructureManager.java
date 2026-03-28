package builderb0y.bigglobe.structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.ValkyrienSkiesCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ColumnValueOverrider;
import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;
import builderb0y.bigglobe.util.LinkedArrayList;
import builderb0y.bigglobe.util.TimestampedComputingCache;
import builderb0y.bigglobe.util.TimestampedComputingCache.Units;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.util.CollectionTransformer;

public class ActiveStructureManager extends StructureManager {

	public final TimestampedComputingCache<ChunkPos, FinalStructures>
		intersectingStructures = new TimestampedComputingCache<>(Units.seconds(60.0D), Units.gigabytes(1.0D)),
		finalStructures = new TimestampedComputingCache<>(Units.seconds(60.0D), Units.gigabytes(1.0D));
	public final TimestampedComputingCache<StructureKey, SectionSortedStructurePieces>
		potentialStructures = new TimestampedComputingCache<>(Units.seconds(60.0D), Units.gigabytes(1.0D));
	public final Object2ObjectOpenHashMap<Holder<StructureSet>, OptionalInt>
		structureSetSizeCache = new Object2ObjectOpenHashMap<>();
	public final ReentrantReadWriteLock
		sizeCacheLock = new ReentrantReadWriteLock();

	public static <T> Stream<T> maybeParallel(Stream<T> stream) {
		return BigGlobeConfig.INSTANCE.get().c2meIntegration.multiThreadedStructures() ? stream.parallel() : stream.sequential();
	}

	public OptionalInt maxSize(Holder<StructureSet> set) {
		OptionalInt result;
		this.sizeCacheLock.readLock().lock();
		try {
			result = this.structureSetSizeCache.get(set);
		}
		finally {
			this.sizeCacheLock.readLock().unlock();
		}
		if (result == null) {
			this.sizeCacheLock.writeLock().lock();
			try {
				result = this.structureSetSizeCache.computeIfAbsent(
					set,
					(Holder<StructureSet> set_) -> (
						set_
							.value()
							.structures()
							.stream()
							.map(StructureSelectionEntry::structure)
							.map(Holder<Structure>::value)
							.map(SizedStructure.class::cast)
							.mapToInt(SizedStructure::bigglobe_getMaxRadiusInChunks)
							.max()
					)
				);
			}
			finally {
				this.sizeCacheLock.writeLock().unlock();
			}
		}
		return result;
	}

	public static Stream<StructureKey> getFilteredStartChunks(StructureGenerationParams params, Holder<StructureSet> set, int radius) {
		return maybeParallel(
			(
				(StreamableStructurePlacement)(
					set.value().placement()
				)
			)
				.bigglobe_getFilteredStartChunks(
					params.generator(),
					params.structurePlacementCalculator(),
					params.chunkPos().x,
					params.chunkPos().z,
					radius
				)
		)
				.map((ChunkPos chunkPos) -> new StructureKey(chunkPos.x, chunkPos.z, set));
	}

	@Override
	public FinalStructures getIntersectingStructures(StructureGenerationParams params) {
		return this.intersectingStructures.computeIfUnknown(
			params.chunkPos(), (ChunkPos pos) -> {
				return this.computeIntersectingStructures(params);
			}
		);
	}

	public FinalStructures computeIntersectingStructures(StructureGenerationParams params) {
		return (
			maybeParallel(
				params
					.structurePlacementCalculator()
					.possibleStructureSets()
					.stream()
			)
				.flatMap((Holder<StructureSet> set) -> {
					return this.maxSize(set).stream().mapToObj((int radius) -> getFilteredStartChunks(params, set, radius)).flatMap(Function.identity());
				})
				.map(StructureKey::chunkPos)
				.distinct()
				.flatMap((ChunkPos pos) -> maybeParallel(this.getFinalStructures(params.at(pos)).stream()))
				.filter((StructureStart start) -> (
					params.chunkPos().x >= start.getBoundingBox().minX() >> 4 &&
					params.chunkPos().z >= start.getBoundingBox().minZ() >> 4 &&
					params.chunkPos().x <= start.getBoundingBox().maxX() >> 4 &&
					params.chunkPos().z <= start.getBoundingBox().maxZ() >> 4
				))
				.sorted(
					Comparator
						.comparing((StructureStart start) -> start.getStructure().step())
						.thenComparing((StructureStart start) -> structureID(start.getStructure()))
				)
				.collect(Collectors.toCollection(FinalStructures::new))
		);
	}

	@Override
	public ScriptStructures[] computeRelevantStructuresForOverriders(StructureGenerationParams params, ColumnValueOverridersWithRadiusCache overriders) {
		//note: need an actual Registry instance here due to its
		//ability to lookup the key associated with a given object.
		Registry<Structure> structureRegistry = RegistryVersions.getRegistry(
			params.dynamicRegistries(),
			Registries.STRUCTURE
		);
		@SuppressWarnings("unchecked")
		@Nullable List<@NotNull StructureStartWrapper> @NotNull [] intermediate = new List[overriders.overriders().length];
		params
			.structurePlacementCalculator()
			.possibleStructureSets()
			.stream()
			.flatMap((Holder<StructureSet> set) -> {
				OptionalInt size = this.maxSize(set);
				if (size.isEmpty()) return Stream.empty();
				int extra = overriders.getSearchRadius(set);
				if (extra < 0) return Stream.empty();
				return getFilteredStartChunks(params, set, size.getAsInt() + extra);
			})
			.map(StructureKey::chunkPos)
			.distinct()
			.flatMap((ChunkPos pos) -> this.getFinalStructures(params.at(pos)).stream())
			.sorted(
				Comparator
					.comparing((StructureStart start) -> start.getStructure().step())
					.thenComparing((StructureStart start) -> structureID(start.getStructure()))
			)
			.forEachOrdered((StructureStart start) -> {
				for (int index : overriders.getIndices(start.getStructure())) {
					List<StructureStartWrapper> structures = intermediate[index];
					if (structures == null) {
						structures = intermediate[index] = new ArrayList<>();
					}
					ColumnValueOverrider.Entry overrider = overriders.overriders()[index].value();
					int extra = overrider.getSearchRadius(start.getStructure());
					if (
						(start.getBoundingBox().minX() >> 4) - extra <= params.chunkPos().x &&
						(start.getBoundingBox().minZ() >> 4) - extra <= params.chunkPos().z &&
						(start.getBoundingBox().maxX() >> 4) + extra >= params.chunkPos().x &&
						(start.getBoundingBox().maxZ() >> 4) + extra >= params.chunkPos().z
					) {
						structures.add(
							StructureStartWrapper.of(
								RegistryVersions.getEntry(
									structureRegistry,
									start.getStructure()
								),
								start
							)
						);
					}
				}
			});
		return CollectionTransformer.convertArray(
			intermediate,
			ScriptStructures[]::new,
			(List<StructureStartWrapper> wrappers) -> (
				wrappers == null
					? ScriptStructures.EMPTY_SCRIPT_STRUCTURES
					: new ScriptStructures(
					wrappers.toArray(
						StructureStartWrapper.ARRAY_FACTORY
					)
				)
			)
		);
	}

	@Override
	public FinalStructures getFinalStructures(StructureGenerationParams params) {
		return this.finalStructures.computeIfUnknown(
			params.chunkPos(), (ChunkPos chunkPos) -> {
				return this.computeFinalStructures(params);
			}
		);
	}

	public FinalStructures computeFinalStructures(StructureGenerationParams params) {
		LinkedArrayList<SectionSortedStructurePieces> starts = new LinkedArrayList<>();
		MutableInt maxSizeForChunk = new MutableInt(0);

		//step 1: get all the structures that start in the current chunk.
		//also compute the size of the largest one.
		maybeParallel(
			params
				.structurePlacementCalculator()
				.possibleStructureSets()
				.stream()
		)
			.flatMap((Holder<StructureSet> set) -> {
				return getFilteredStartChunks(params, set, 0);
			})
			.forEach((StructureKey key) -> {
				SectionSortedStructurePieces pieces = this.getPotentialStructures(params.at(key.chunkX(), key.chunkZ()), key.set());
				if (!pieces.isEmpty()) synchronized (starts) {
					starts.addElementToEnd(pieces);
					this.maxSize(key.set()).ifPresent((int size) -> maxSizeForChunk.setValue(Math.max(maxSizeForChunk.getValue(), size)));
				}
			});
		if (starts.isEmpty()) return new FinalStructures(0);

		//step 2: get all the structures which could potentially collide with anything in the current chunk.
		maybeParallel(
			params
				.structurePlacementCalculator()
				.possibleStructureSets()
				.stream()
		)
			.flatMap((Holder<StructureSet> set) -> {
				return this.maxSize(set).stream().mapToObj((int radius) -> {
						return getFilteredStartChunks(params, set, maxSizeForChunk.getValue() + radius);
					})
						.flatMap(Function.identity());
			})
			.filter((StructureKey key) -> key.chunkX() != params.chunkPos().x || key.chunkZ() != params.chunkPos().z)
			.forEach((StructureKey key) -> {
				SectionSortedStructurePieces pieces = this.getPotentialStructures(params.at(key.chunkX(), key.chunkZ()), key.set());
				if (!pieces.isEmpty()) synchronized (starts) {
					starts.addElementToEnd(pieces);
				}
			});

		//step 3: filter, log, and convert.
		LinkedArrayList<SectionSortedStructurePieces> filtered = removeIntersections(params, starts);
		if (filtered.isEmpty()) return new FinalStructures(0);
		FinalStructures result = new FinalStructures(filtered.size());
		while (!filtered.isEmpty()) {
			StructureStart start = filtered.removeFirstElement().startWrapper.start();
			if (canLog(start.getStructure())) {
				BigGlobeMod.LOGGER.info("Structure " + structureName(start.getStructure()) + " spawned at " + start.getBoundingBox().getCenter());
			}
			result.add(start);
		}
		return result;
	}

	public static LinkedArrayList<SectionSortedStructurePieces> removeIntersections(StructureGenerationParams params, LinkedArrayList<SectionSortedStructurePieces> list) {
		SortedOverriders overriders = params.generator().getOverriders();
		LinkedArrayList<SectionSortedStructurePieces> filtered = new LinkedArrayList<>();
		outer:
		for (LinkedArrayList.Node<SectionSortedStructurePieces> current = list.first; current != null; current = current.next) {
			if (current.element.startWrapper.start().getChunkPos().equals(params.chunkPos())) {
				for (LinkedArrayList.Node<SectionSortedStructurePieces> other = list.first; other != null; other = other.next) {
					if (other == current) continue;
					if (SectionSortedStructurePieces.intersects(current.element, other.element)) {
						int priority = overriders.getCollisionPriority(params.columns(), current.element.startWrapper, other.element.startWrapper);
						if (priority < 0) {
							if (canLog(current.element.startWrapper.structure().entry)) {
								BigGlobeMod.LOGGER.info("Structure " + current.element + " did not spawn because it collided with a " + other.element + " and a collision overrider returned a negative priority.");
							}
							continue outer;
						}
						else if (priority == 0 && current.element.volume <= other.element.volume) {
							if (canLog(current.element.startWrapper.structure().entry)) {
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

	public @NotNull SectionSortedStructurePieces getPotentialStructures(StructureGenerationParams params, Holder<StructureSet> set) {
		return this.potentialStructures.computeIfUnknown(
			new StructureKey(params.chunkPos().x, params.chunkPos().z, set), (StructureKey chunkPos) -> {
				return this.computePotentialStructures(params, set);
			}
		);
	}

	public @NotNull SectionSortedStructurePieces computePotentialStructures(StructureGenerationParams params, Holder<StructureSet> set) {
		StructureStartWrapper wrapper = this.computeStructureStart(params, set);
		if (wrapper != null) {
			return new SectionSortedStructurePieces(set, wrapper);
		}
		else {
			return new SectionSortedStructurePieces(set);
		}
	}

	public @Nullable StructureStartWrapper computeStructureStart(StructureGenerationParams params, Holder<StructureSet> set) {
		Permuter structureChooser = new Permuter(0L);
		long chunkSeed = Permuter.permute(params.columnSeed() ^ 0x767DB826EDD5532EL, params.chunkPos().x, params.chunkPos().z);
		structureChooser.setSeed(Permuter.permute(chunkSeed, UnregisteredObjectException.getID(set).hashCode()));
		List<StructureSelectionEntry> possibilities = new ArrayList<>(set.value().structures());
		int totalWeight = getTotalWeight(possibilities);
		while (!possibilities.isEmpty()) {
			int index = getRandomIndex(possibilities, totalWeight, structureChooser);
			StructureSelectionEntry entry = possibilities.get(index);
			StructureStartWrapper structure = this.computeStructureStart(params, entry);
			if (structure != null && structure.start().isValid()) {
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

	public static int getTotalWeight(List<StructureSelectionEntry> list) {
		int sum = 0;
		for (int index = 0, size = list.size(); index < size; index++) {
			sum += list.get(index).weight();
		}
		return sum;
	}

	public static int getRandomIndex(List<StructureSelectionEntry> list, int totalWeight, RandomGenerator random) {
		int rng = random.nextInt(totalWeight);
		for (int index = 0, size = list.size(); index < size; index++) {
			if ((rng -= list.get(index).weight()) < 0) {
				return index;
			}
		}
		throw new IllegalStateException("either the RandomGenerator messed up, or the weights changed.");
	}

	public @Nullable StructureStartWrapper computeStructureStart(StructureGenerationParams params, StructureSelectionEntry weightedEntry) {
		if (ValkyrienSkiesCompat.isInShipyard(params.chunkPos())) {
			if (canLog(weightedEntry.structure())) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because Valkyrien Skies reserves this area.");
			}
			return null;
		}

		Structure structure = weightedEntry.structure().value();
		HolderSet<Biome> validBiomes = structure.biomes();
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate().value();
		}

		GenerationStub newStartPosition;
		try {
			newStartPosition = structure.findValidGenerationPoint(
					params.toStructureContext(Predicates.alwaysTrue())
				)
								.orElse(null);
		}
		catch (Exception exception) {
			Identifier structureID = structureID(structure);
			BigGlobeMod.LOGGER.error("Caught exception while generating StructurePosition for structure " + structureID + ". Please report this to the developers of mod " + structureID.getNamespace(), exception);
			return null;
		}

		if (newStartPosition == null) {
			if (canLog(weightedEntry.structure())) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because the structure itself decided not to.");
			}
			return null;
		}

		StructurePiecesBuilder collector;
		try {
			collector = newStartPosition.getPiecesBuilder();
		}
		catch (Exception exception) {
			Identifier structureID = structureID(structure);
			BigGlobeMod.LOGGER.error("Caught exception while generating StructurePiecesCollector for structure " + structureID + ". Please report this to the developers of mod " + structureID.getNamespace(), exception);
			return null;
		}

		StructureStart newStart = new StructureStart(weightedEntry.structure().value(), params.chunkPos(), 0, collector.build());
		if (!newStart.isValid()) {
			if (canLog(weightedEntry.structure())) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because the resulting structure has no pieces.");
			}
			return null;
		}
		int structureX = Interpolator.clamp(newStart.getBoundingBox().minX(), newStart.getBoundingBox().maxX(), newStartPosition.position().getX());
		int structureY = Interpolator.clamp(newStart.getBoundingBox().minY(), newStart.getBoundingBox().maxY(), newStartPosition.position().getY());
		int structureZ = Interpolator.clamp(newStart.getBoundingBox().minZ(), newStart.getBoundingBox().maxZ(), newStartPosition.position().getZ());
		if (
			(
				newStartPosition.position().getX() != structureX ||
				newStartPosition.position().getY() != structureY ||
				newStartPosition.position().getZ() != structureZ
			)
			&& canLog(weightedEntry.structure())
		) {
			BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " has a position outside its bounding box. This may lead to unexpected results. It will now be clamped.");
		}

		StructureStartWrapper wrapper = StructureStartWrapper.of(weightedEntry.structure(), newStart);
		int oldY = newStart.getBoundingBox().minY();
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
						params.chunkPos().x,
						params.chunkPos().z
					)
				)
			)
		) {
			//canStructureSpawn() logs a message on its own.
			return null;
		}

		int newY = newStart.getBoundingBox().minY();
		Holder<Biome> biome = params.biomeSource().getNoiseBiome(
			(structureX) >> 2,
			(structureY + (newY - oldY)) >> 2,
			(structureZ) >> 2,
			params.noiseConfig().sampler()
		);

		if (!validBiomes.contains(biome)) {
			if (canLog(weightedEntry.structure())) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn at " + newStart.getBoundingBox().getCenter() + " because the biome at this location (" + biome + ") is not in the structure's biome tag.");
			}
			return null;
		}

		return wrapper;
	}

	public boolean canStructureSpawn(
		StructureGenerationParams params,
		StructureStartWrapper wrapper,
		StructureStart start,
		Permuter permuter
	) {
		Hints hints = ColumnUsage.GENERIC.maybeDhHints(params.distantHorizons());
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(
			params.generator().columnEntryRegistry.columnFactory,
			new ScriptedColumn.Params(params.generator(), 0, 0, hints)
		);
		for (StructureOverrider.Entry overrider : params.generator().getOverriders().structures) {
			if (!overrider.script().override(lookup, wrapper, permuter, params.generator().columnSeed)) {
				if (canLog(start.getStructure())) {
					BigGlobeMod.LOGGER.info(
						"Structure " +
						structureName(start.getStructure()) +
						" did not spawn at " +
						start.getBoundingBox().getCenter() +
						" because overrider " +
						RegistryVersions.getRegistry(
								BigGlobeMod.getCurrentServer().registryAccess(),
								BigGlobeDynamicRegistries.OVERRIDER_REGISTRY_KEY
							)
							.getKey(overrider) +
						" said no."
					);
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public StructureManager copy() {
		return new ActiveStructureManager();
	}
}