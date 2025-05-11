package builderb0y.bigglobe.structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import builderb0y.bigglobe.BigGlobeMod;
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
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;
import builderb0y.bigglobe.util.LinkedArrayList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public class ActiveStructureManager extends StructureManager {

	public final UngeneratedStructures<StructureKey, SectionSortedStructurePieces>
		potentialStructures    = new UngeneratedStructures<>(5L * 60L * 1000L);
	public final UngeneratedStructures<ChunkPos, FinalStructures>
		finalStructures        = new UngeneratedStructures<>(5L * 60L * 1000L),
		intersectingStructures = new UngeneratedStructures<>(5L * 60L * 1000L);

	public static <T> Stream<T> maybeParallel(Stream<T> stream) {
		return BigGlobeConfig.INSTANCE.get().c2meIntegration.multiThreadedStructures() ? stream.parallel() : stream.sequential();
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
		FinalStructures starts;
		synchronized (this.intersectingStructures) {
			starts = this.intersectingStructures.get(params.chunkPos());
		}
		if (starts != null) return starts;
		starts = this.computeIntersectingStructures(params);
		synchronized (this.intersectingStructures) {
			FinalStructures existing = this.intersectingStructures.putIfAbsent(params.chunkPos(), starts);
			if (existing != null) starts = existing;
		}
		return starts;
	}

	public FinalStructures computeIntersectingStructures(StructureGenerationParams params) {
		return (
			maybeParallel(
				params
				.structurePlacementCalculator()
				.getStructureSets()
				.stream()
			)
			.flatMap((RegistryEntry<StructureSet> set) -> {
				return maxSize(set).stream().mapToObj((int radius) -> getFilteredStartChunks(params, set, radius)).flatMap(Function.identity());
			})
			.map(StructureKey::chunkPos)
			.distinct()
			.flatMap((ChunkPos pos) -> maybeParallel(this.getStructureStarts(params.at(pos)).stream()))
			.filter((StructureStart start) -> (
				params.chunkPos().x >= start.getBoundingBox().getMinX() >> 4 &&
				params.chunkPos().z >= start.getBoundingBox().getMinZ() >> 4 &&
				params.chunkPos().x <= start.getBoundingBox().getMaxX() >> 4 &&
				params.chunkPos().z <= start.getBoundingBox().getMaxZ() >> 4
			))
			.sorted(
				Comparator.comparing(
					(StructureStart start) -> start.getStructure().getFeatureGenerationStep()
				)
				.thenComparing(
					(StructureStart start) -> structureID(start.getStructure())
				)
			)
			.collect(Collectors.toCollection(FinalStructures::new))
		);
	}

	@Override
	public FinalStructures getStructureStarts(StructureGenerationParams params) {
		FinalStructures finalStructures;
		synchronized (this.finalStructures) {
			finalStructures = this.finalStructures.get(params.chunkPos());
		}
		if (finalStructures != null) return finalStructures;
		finalStructures = this.computeStructureStarts(params);
		synchronized (this.finalStructures) {
			FinalStructures existing = this.finalStructures.putIfAbsent(params.chunkPos(), finalStructures);
			if (existing != null) finalStructures = existing;
		}
		return finalStructures;
	}

	public FinalStructures computeStructureStarts(StructureGenerationParams params) {
		LinkedArrayList<SectionSortedStructurePieces> starts = new LinkedArrayList<>();
		MutableInt maxSizeForChunk = new MutableInt(0);

		//step 1: get all the structures that start in the current chunk.
		//also compute the size of the largest one.
		maybeParallel(
			params
			.structurePlacementCalculator()
			.getStructureSets()
			.stream()
		)
		.flatMap((RegistryEntry<StructureSet> set) -> {
			return getFilteredStartChunks(params, set, 0);
		})
		.forEach((StructureKey key) -> {
			SectionSortedStructurePieces pieces = this.getStructureStart(params.at(key.chunkX(), key.chunkZ()), key.set());
			if (!pieces.isEmpty()) synchronized (starts) {
				starts.addElementToEnd(pieces);
				maxSize(key.set()).ifPresent((int size) -> maxSizeForChunk.setValue(Math.max(maxSizeForChunk.getValue(), size)));
			}
		});
		if (starts.isEmpty()) return new FinalStructures(0);

		//step 2: get all the structures which could potentially collide with anything in the current chunk.
		maybeParallel(
			params
			.structurePlacementCalculator()
			.getStructureSets()
			.stream()
		)
		.flatMap((RegistryEntry<StructureSet> set) -> {
			return maxSize(set).stream().mapToObj((int radius) -> {
				return getFilteredStartChunks(params, set, maxSizeForChunk.getValue() + radius);
			})
			.flatMap(Function.identity());
		})
		.filter((StructureKey key) -> key.chunkX() != params.chunkPos().x || key.chunkZ() != params.chunkPos().z)
		.forEach((StructureKey key) -> {
			SectionSortedStructurePieces pieces = this.getStructureStart(params.at(key.chunkX(), key.chunkZ()), key.set());
			if (!pieces.isEmpty()) synchronized (starts) {
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
		SortedOverriders overriders = params.generator().getOverriders();
		LinkedArrayList<SectionSortedStructurePieces> filtered = new LinkedArrayList<>();
		outer:
		for (LinkedArrayList.Node<SectionSortedStructurePieces> current = list.first; current != null; current = current.next) {
			if (current.element.startWrapper.start().getPos().equals(params.chunkPos())) {
				for (LinkedArrayList.Node<SectionSortedStructurePieces> other = list.first; other != null; other = other.next) {
					if (other == current) continue;
					if (SectionSortedStructurePieces.intersects(current.element, other.element)) {
						int priority = overriders.getCollisionPriority(params.columns(), current.element.startWrapper, other.element.startWrapper);
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
		StructureKey key = new StructureKey(params.chunkPos().x, params.chunkPos().z, set);
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
		long chunkSeed = Permuter.permute(params.columnSeed() ^ 0x767DB826EDD5532EL, params.chunkPos().x, params.chunkPos().z);
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
		if (ValkyrienSkiesCompat.isInShipyard(params.chunkPos())) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because Valkyrien Skies reserves this area.");
			}
			return null;
		}

		Structure structure = weightedEntry.structure().value();
		RegistryEntryList<Biome> validBiomes = structure.getValidBiomes();
		while (structure instanceof DelegatingStructure delegating && delegating.canDelegateStart()) {
			structure = delegating.delegate().value();
		}

		StructurePosition newStartPosition;
		try {
			newStartPosition = structure.getValidStructurePosition(
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
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because the structure itself decided not to.");
			}
			return null;
		}

		StructurePiecesCollector collector;
		try {
			collector = newStartPosition.generate();
		}
		catch (Exception exception) {
			Identifier structureID = structureID(structure);
			BigGlobeMod.LOGGER.error("Caught exception while generating StructurePiecesCollector for structure " + structureID + ". Please report this to the developers of mod " + structureID.getNamespace(), exception);
			return null;
		}

		StructureStart newStart = new StructureStart(weightedEntry.structure().value(), params.chunkPos(), 0, collector.toList());
		if (!newStart.hasChildren()) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn in chunk " + params.chunkPos() + " because the resulting structure has no pieces.");
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
						params.chunkPos().x,
						params.chunkPos().z
					)
				)
			)
		) {
			//canStructureSpawn() logs a message on its own.
			return null;
		}

		int newY = newStart.getBoundingBox().getMinY();
		RegistryEntry<Biome> biome = params.biomeSource().getBiome(
			newStartPosition.position().getX() >> 2,
			(newStartPosition.position().getY() + (newY - oldY)) >> 2,
			newStartPosition.position().getZ() >> 2,
			params.noiseConfig().getMultiNoiseSampler()
		);

		if (!validBiomes.contains(biome)) {
			if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.structureSpawning) {
				BigGlobeMod.LOGGER.info("Structure " + UnregisteredObjectException.getID(weightedEntry.structure()) + " did not spawn at " + newStart.getBoundingBox().getCenter() + " because the biome at this location (" + biome + ") is not in the structure's biome tag.");
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
		Hints hints = ColumnUsage.GENERIC.maybeDhHints(params.distantHorizons());
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(
			params.generator().columnEntryRegistry.columnFactory,
			new ScriptedColumn.Params(params.generator(), 0, 0, hints)
		);
		for (StructureOverrider.Entry overrider : params.generator().getOverriders().structures) {
			if (!overrider.script().override(lookup, wrapper, permuter, params.generator().columnSeed)) {
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

	@Override
	public StructureManager copy() {
		return new ActiveStructureManager();
	}
}