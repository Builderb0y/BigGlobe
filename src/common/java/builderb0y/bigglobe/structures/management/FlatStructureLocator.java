package builderb0y.bigglobe.structures.management;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.ScriptedColumnBiomeSource;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.InstalledMods;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.overriders.Overrider.SortedOverriders;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.SizedStructure;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresManyBoxes;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresOneBox;
import builderb0y.bigglobe.util.BudgetStableValue;
import builderb0y.bigglobe.util.Grouper;
import builderb0y.bigglobe.util.Streamable;
import builderb0y.bigglobe.util.Streamable.StreamableArrayList;
import builderb0y.bigglobe.util.TimestampedComputingCache;
import builderb0y.bigglobe.util.TimestampedComputingCache.Units;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public class FlatStructureLocator extends StructureLocator {

	public static final Executor STRUCTURE_EXECUTOR = InstalledMods.C2ME ? ForkJoinPool.commonPool() : Executors.newSingleThreadExecutor((Runnable task) -> {
		Thread thread = new Thread(task, "Big Globe Structure Thread");
		thread.setDaemon(true);
		return thread;
	});

	public final TimestampedComputingCache<StructurePos, StructureCaches>
		caches = new TimestampedComputingCache<>(Units.minutes(5.0D), Units.gigabytes(1.0D));
	public final Map<Holder<Structure>, List<Holder<StructureSet>>>
		structureToSets;
	public StructureCaches
		mostRecentCache;

	public FlatStructureLocator(ChunkGeneratorStructureState structureState) {
		this.structureToSets = (
			structureState
			.possibleStructureSets()
			.stream()
			.collect(
				Grouper.groupingToList(
					Grouper.keysPerElement(
						(Holder<StructureSet> holder) -> (
							holder
							.value()
							.structures()
							.stream()
							.map(StructureSelectionEntry::structure)
						)
					),
					Grouper.valueElement()
				)
			)
		);
	}

	public StructureCaches getCaches(StructurePos pos) {
		StructureCaches cache = this.mostRecentCache;
		if (cache != null && cache.pos.equals(pos)) {
			return cache;
		}
		else {
			return this.mostRecentCache = this.caches.computeIfUnknown(pos, StructureCaches::new);
		}
	}

	public StructureCaches getCachesBulk(StructurePos pos) {
		return this.caches.computeIfUnknown(pos, StructureCaches::new);
	}

	@Override
	public Streamable<Holder<Structure>> allStructures() {
		return this.new AllStructures();
	}

	public class AllStructures implements Streamable<Holder<Structure>> {

		@Override
		public Stream<Holder<Structure>> stream() {
			return FlatStructureLocator.this.structureToSets.keySet().stream();
		}
	}

	public static record FilteredStructureCaches(StructureCaches caches, Streamable<Holder<Structure>> filter) {}

	public Stream<StructurePos> getPotentialCaches(
		Context context,
		Streamable<Holder<Structure>> structures,
		BoundingBox box,
		boolean strict
	) {
		if (structures instanceof AllStructures) {
			return StructurePos.intersectingArea(box);
		}
		else {
			return (
				structures
				.stream()
				.map(this.structureToSets::get)
				.filter(Objects::nonNull)
				.flatMap(List<Holder<StructureSet>>::stream)
				.flatMap((Holder<StructureSet> set) -> (
					(
						(SmartStructurePlacement)(
							set.value().placement()
						)
					)
					.bigglobe_generateStructuresInArea(
						new SmartStructurePlacement.Context(
							this,
							context,
							set,
							context.columnSource().lookup(),
							box,
							strict
						)
					)
				))
				.map(StructureStartWrapper::pos)
				.map(StructurePos::fromBlock)
			);
		}
	}

	public Streamable<FilteredStructureCaches> sortPositions(
		Params params,
		@Nullable Comparator<StructurePos> order,
		boolean strict
	) {
		return switch (params.whatToSearchFor()) {
			case ManyStructuresOneBox oneBox -> {
				StructurePos pos = StructurePos.fromAreaIfOnlyOne(oneBox.box);
				if (pos != null) {
					yield Streamable.singleton(new FilteredStructureCaches(this.getCaches(pos), oneBox.structures));
				}
				else {
					StreamableArrayList<FilteredStructureCaches> result = (
						this
						.getPotentialCaches(
							params.context(),
							params.whatToSearchFor().structures,
							oneBox.box,
							strict
						)
						.map(this::getCachesBulk)
						.map((StructureCaches localCaches) -> new FilteredStructureCaches(localCaches, oneBox.structures))
						.collect(Collectors.toCollection(StreamableArrayList::new))
					);
					if (order != null) result.sort(Comparator.comparing((FilteredStructureCaches filtered) -> filtered.caches.pos, order));
					yield result;
				}
			}
			case ManyStructuresManyBoxes manyBoxes -> {
				Set<Map.Entry<StructureCaches, StreamableArrayList<Holder<Structure>>>> entries = (
					manyBoxes
					.streamStructures()
					.collect(
						new Grouper<>(
							HashMap::new,
							Grouper.keysPerElement((Holder<Structure> structure) -> {
								return (
									this
									.getPotentialCaches(
										params.context(),
										Streamable.singleton(structure),
										manyBoxes.getAreaFor(structure),
										strict
									)
									.map(this::getCachesBulk)
								);
							}),
							Grouper.valueElement(),
							Grouper.toCollection(StreamableArrayList::new),
							0
						)
					)
					.entrySet()
				);
				if (order != null) {
					StreamableArrayList<FilteredStructureCaches> list = new StreamableArrayList<>(entries.size());
					for (Map.Entry<StructureCaches, StreamableArrayList<Holder<Structure>>> entry : entries) {
						list.add(new FilteredStructureCaches(entry.getKey(), entry.getValue()));
					}
					list.sort(Comparator.comparing((FilteredStructureCaches filtered) -> filtered.caches.pos, order));
					yield list;
				}
				else {
					yield () -> entries.stream().map((Map.Entry<StructureCaches, StreamableArrayList<Holder<Structure>>> entry) -> new FilteredStructureCaches(entry.getKey(), entry.getValue()));
				}
			}
		};
	}

	public Stream<StructureStartWrapper> commonLocate(Params params, Comparator<StructurePos> order, boolean strict, boolean eager) {
		Stream<CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>> stream = (
			this
			.sortPositions(params, order, strict)
			.stream()
			.map((FilteredStructureCaches filtered) -> {
				return filtered.caches.getFiltered(params.context());
			})
		);
		Stream<CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>> stream2;
		if (eager) {
			stream2 = stream;
		}
		else {
			@SuppressWarnings("unchecked")
			CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>[] futures = (
				stream.toArray(CompletableFuture[]::new)
			);
			CompletableFuture.allOf(futures).join();
			stream2 = Arrays.stream(futures);
		}
		return (
			stream2
			.map(CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>::join)
			.flatMap((Map<Holder<Structure>, List<ChunkSortedStructurePieces>> map) -> {
				return (
					params
					.whatToSearchFor()
					.streamStructures()
					.flatMap((Holder<Structure> holder) -> {
						List<ChunkSortedStructurePieces> list = map.get(holder);
						if (list == null) return Stream.empty();
						Stream<ChunkSortedStructurePieces> result = list.stream();
						if (strict) result = result.filter((ChunkSortedStructurePieces pieces) -> {
							return pieces.startWrapper.box().intersects(params.whatToSearchFor().getAreaFor(holder));
						});
						return result;
					})
					.map((ChunkSortedStructurePieces pieces) -> pieces.startWrapper)
				);
			})
		);
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresInside(Params params) {
		return this.commonLocate(params, null, true, false);
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresNearby(Params params, BlockPos center) {
		return this.commonLocate(params, StructurePos.comparingByDistanceTo(center), false, true);
	}

	@Override
	public Stream<StructureStartWrapper> getStructuresIntersecting(Params params) {
		return switch (params.whatToSearchFor()) {
			case ManyStructuresOneBox oneBox -> {
				StructurePos pos = StructurePos.fromAreaIfOnlyOne(oneBox.box);
				if (pos != null) {
					Map<Holder<Structure>, List<ChunkSortedStructurePieces>> map = this.getCaches(pos).getIntersecting(params.context()).join();
					yield (
						params
						.whatToSearchFor()
						.streamStructures()
						.map(map::get)
						.filter(Objects::nonNull)
						.flatMap(List<ChunkSortedStructurePieces>::stream)
						.filter((ChunkSortedStructurePieces pieces) -> pieces.startWrapper.box().intersects(oneBox.box))
						.map((ChunkSortedStructurePieces pieces) -> pieces.startWrapper)
					);
				}
				else {
					@SuppressWarnings("unchecked") //generic array.
					CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>[] futures = (
						StructurePos
						.intersectingArea(oneBox.box)
						.map(this::getCachesBulk)
						.map((StructureCaches caches) -> caches.getFiltered(params.context()))
						.toArray(CompletableFuture[]::new)
					);
					CompletableFuture.allOf(futures).join();
					yield (
						Arrays
						.stream(futures)
						.map(CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>::join)
						.flatMap((Map<Holder<Structure>, List<ChunkSortedStructurePieces>> map) -> {
							return (
								params
								.whatToSearchFor()
								.streamStructures()
								.map(map::get)
								.filter(Objects::nonNull)
								.flatMap(List<ChunkSortedStructurePieces>::stream)
								.filter((ChunkSortedStructurePieces pieces) -> pieces.startWrapper.box().intersects(oneBox.box))
								.map((ChunkSortedStructurePieces pieces) -> pieces.startWrapper)
							);
						})
					);
				}
			}
			case ManyStructuresManyBoxes manyBoxes -> {
				Map<StructurePos, List<Holder<Structure>>> needed = manyBoxes.streamStructures().collect(
					Grouper.groupingToList(
						Grouper.keysPerElement((Holder<Structure> structure) -> {
							int expansion = ((SizedStructure)(structure.value())).bigglobe_getMaxRadiusInBlocks();
							return StructurePos.intersectingArea(
								manyBoxes
								.getAreaFor(structure)
								.inflatedBy(expansion, 0, expansion)
							);
						}),
						Grouper.valueElement()
					)
				);
				@SuppressWarnings("unchecked") //generic array.
				CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>[] futures = (
					needed
					.keySet()
					.stream()
					.map(this::getCachesBulk)
					.map((StructureCaches caches) -> caches.getFiltered(params.context()))
					.toArray(CompletableFuture[]::new)
				);
				CompletableFuture.allOf(futures).join();
				yield (
					Arrays
					.stream(futures)
					.map(CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>::join)
					.flatMap((Map<Holder<Structure>, List<ChunkSortedStructurePieces>> map) -> {
						return (
							params
							.whatToSearchFor()
							.streamStructures()
							.flatMap((Holder<Structure> holder) -> {
								List<ChunkSortedStructurePieces> list = map.get(holder);
								if (list == null) return Stream.empty();
								return list.stream().filter((ChunkSortedStructurePieces pieces) -> {
									return pieces.startWrapper.box().intersects(manyBoxes.getAreaFor(holder));
								});
							})
							.map((ChunkSortedStructurePieces pieces) -> pieces.startWrapper)
						);
					})
				);
			}
		};
	}

	@Override
	public @Nullable WeightedList<SpawnerData> getMobSpawns(Context context, BlockPos blockPos, MobCategory group) {
		BoundingBox box = null;
		CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>> intersecting = this.getCaches(StructurePos.fromBlock(blockPos)).getIntersecting(context);
		for (List<ChunkSortedStructurePieces> list : intersecting.join().values()) {
			for (ChunkSortedStructurePieces pieces : list) {
				if (pieces.startWrapper.box().isInside(blockPos)) {
					StructureSpawnOverride override = pieces.startWrapper.originalStructure().value().spawnOverrides().get(group);
					if (override != null) {
						if (override.boundingBox() == BoundingBoxType.PIECE) {
							if (box == null) {
								box = new BoundingBox(blockPos);
							}
							if (!pieces.intersects(box)) {
								continue;
							}
						}
						return override.spawns();
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean maybeHasBiomes(BiomeParams params) {
		if (params.predicate() == Predicates.<Holder<Biome>>alwaysTrue()) return true;
		return (
			StructurePos
			.intersectingArea(params.area())
			.map(this::getCaches)
			.anyMatch((StructureCaches caches) -> caches.hasBiome(params))
		);
	}

	public static <T> CompletableFuture<T> submitLater(Supplier<T> supplier, CompletableFuture<?>... dependencies) {
		CompletableFuture<T> future = new CompletableFuture<>();
		CompletableFuture.allOf(dependencies).whenComplete((Void _, Throwable _) -> {
			future.completeAsync(supplier, STRUCTURE_EXECUTOR);
		});
		return future;
	}

	public static record StructurePos(int x, int z) {

		public static final int
			BLOCK_SHIFT = 6,
			CHUNK_SHIFT = 2;

		public static StructurePos fromBlock(int x, int z) {
			return new StructurePos(x >> BLOCK_SHIFT, z >> BLOCK_SHIFT);
		}

		public static StructurePos fromBlock(BlockPos pos) {
			return new StructurePos(pos.getX() >> BLOCK_SHIFT, pos.getZ() >> BLOCK_SHIFT);
		}

		public static StructurePos fromChunk(int x, int z) {
			return new StructurePos(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT);
		}

		public static StructurePos fromChunk(ChunkPos pos) {
			return new StructurePos(pos.x() >> CHUNK_SHIFT, pos.z() >> CHUNK_SHIFT);
		}

		public static @Nullable StructurePos fromAreaIfOnlyOne(BoundingBox box) {
			int
				minX = box.minX() >> BLOCK_SHIFT,
				minZ = box.minZ() >> BLOCK_SHIFT,
				maxX = box.maxX() >> BLOCK_SHIFT,
				maxZ = box.maxZ() >> BLOCK_SHIFT;
			if (minX == maxX && minZ == maxZ) {
				return new StructurePos(minX, minZ);
			}
			else {
				return null;
			}
		}

		public static Stream<StructurePos> intersectingArea(BoundingBox box) {
			int
				minX = box.minX() >> BLOCK_SHIFT,
				minZ = box.minZ() >> BLOCK_SHIFT,
				maxX = box.maxX() >> BLOCK_SHIFT,
				maxZ = box.maxZ() >> BLOCK_SHIFT;
			if (minX == maxX && minZ == maxZ) {
				return Stream.of(new StructurePos(minX, minZ));
			}
			else {
				return IntStream.rangeClosed(minZ, maxZ).mapToObj((int z) -> {
					return IntStream.rangeClosed(minX, maxX).mapToObj((int x) -> {
						return new StructurePos(x, z);
					});
				})
				.flatMap(Function.identity());
			}
		}

		public BoundingBox toAreaFromExclusiveY(int minY, int maxYExclusive) {
			return this.toAreaFromInclusiveY(minY, maxYExclusive - 1);
		}

		public BoundingBox toAreaFromInclusiveY(int minY, int maxYInclusive) {
			return new BoundingBox(
				this.minBlockX(),
				minY,
				this.minBlockZ(),
				this.maxBlockXInclusive(),
				maxYInclusive,
				this.maxBlockZInclusive()
			);
		}

		public BoundingBox toArea(LevelHeightAccessor height) {
			return this.toAreaFromInclusiveY(
				HeightLimitViewVersions.getMinY(height),
				HeightLimitViewVersions.getMaxY(height) - 1
			);
		}

		public static Comparator<StructurePos> comparingByDistanceTo(BlockPos blockPos) {
			return Comparator.comparingLong((StructurePos structurePos) -> {
				int dx = Mth.clamp(blockPos.getX(), structurePos.minBlockX(), structurePos.maxBlockXInclusive()) - blockPos.getX();
				int dz = Mth.clamp(blockPos.getZ(), structurePos.minBlockZ(), structurePos.maxBlockZInclusive()) - blockPos.getZ();
				return BigGlobeMath.squareL(dx, dz);
			});
		}

		public int minBlockX         () { return  this.x      << BLOCK_SHIFT; }
		public int minBlockZ         () { return  this.z      << BLOCK_SHIFT; }
		public int maxBlockXInclusive() { return (this.x      << BLOCK_SHIFT) | ((1 << BLOCK_SHIFT) - 1); }
		public int maxBlockZInclusive() { return (this.z      << BLOCK_SHIFT) | ((1 << BLOCK_SHIFT) - 1); }
		public int minBlockXExclusive() { return (this.x + 1) << BLOCK_SHIFT; }
		public int maxBlockZExclusive() { return (this.z + 1) << BLOCK_SHIFT; }
		public int minChunkX         () { return  this.x      << CHUNK_SHIFT; }
		public int minChunkZ         () { return  this.z      << CHUNK_SHIFT; }
		public int maxChunkXInclusive() { return (this.x      << CHUNK_SHIFT) | ((1 << CHUNK_SHIFT) - 1); }
		public int maxChunkZInclusive() { return (this.z      << CHUNK_SHIFT) | ((1 << CHUNK_SHIFT) - 1); }
		public int minChunkXExclusive() { return (this.x + 1) << CHUNK_SHIFT; }
		public int maxChunkZExclusive() { return (this.z + 1) << CHUNK_SHIFT; }
	}

	public class StructureCaches {

		public final StructurePos
			pos;
		public final BudgetStableValue<Set<Holder<Biome>>>
			biomes       = new BudgetStableValue<>();
		public final BudgetStableValue<CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>>
			unfiltered   = new BudgetStableValue<>(),
			filtered     = new BudgetStableValue<>(),
			intersecting = new BudgetStableValue<>();

		public StructureCaches(StructurePos pos) {
			this.pos = pos;
		}

		public FlatStructureLocator locator() {
			return FlatStructureLocator.this;
		}

		public Set<Holder<Biome>> getBiomes(Context context) {
			return this.biomes.getOrSetBlocking(() -> {
				int
					minX = this.pos.minBlockX() >> 2,
					minZ = this.pos.minBlockZ() >> 2,
					maxX = this.pos.maxBlockXInclusive() >> 2,
					maxZ = this.pos.maxBlockZInclusive() >> 2,
					minY = (context.chunkGenerator().height.min_y()    ) >> 2,
					maxY = (context.chunkGenerator().height.max_y() - 1) >> 2;
				BiomeSource biomeSource = context.chunkGenerator().biome_source();
				if (biomeSource instanceof ScriptedColumnBiomeSource scripted) {
					return (
						IntStream.rangeClosed(minZ, maxZ).parallel().mapToObj((int biomeZ) -> {
							return IntStream.rangeClosed(minX, maxX).parallel().mapToObj((int biomeX) -> {
								ScriptedColumn column = scripted.columnThreadLocal.get();
								column.setParamsUnchecked(column.params.at(biomeX << 2, biomeZ << 2));
								return IntStream.rangeClosed(minY, maxY).sequential().mapToObj((int biomeY) -> {
									return scripted.script.get(column, biomeY << 2).entry;
								});
							});
						})
						.flatMap(Function.identity())
						.flatMap(Function.identity())
						.collect(Collectors.toSet())
					);
				}
				else {
					Sampler sampler = context.structureState().randomState().sampler();
					return (
						IntStream.rangeClosed(minZ, maxZ).parallel().mapToObj((int biomeZ) -> {
							return IntStream.rangeClosed(minX, maxX).parallel().mapToObj((int biomeX) -> {
								return IntStream.rangeClosed(minY, maxY).mapToObj((int biomeY) -> {
									return biomeSource.getNoiseBiome(biomeX, biomeY, biomeZ, sampler);
								});
							});
						})
						.flatMap(Function.identity())
						.flatMap(Function.identity())
						.collect(Collectors.toSet())
					);
				}
			});
		}

		public boolean hasBiome(BiomeParams params) {
			for (Holder<Biome> biome : this.getBiomes(params.context())) {
				if (params.predicate().test(biome)) return true;
			}
			return false;
		}

		public CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>> getUnfiltered(Context context) {
			return this.unfiltered.getOrSetBlocking(() -> CompletableFuture.supplyAsync(
				() -> {
					BoundingBox area = this.pos.toArea(context.height());
					return (
						maybeParallel(
							context
							.structureState()
							.possibleStructureSets()
							.stream()
						)
						.flatMap((Holder<StructureSet> set) -> (
							(
								(SmartStructurePlacement)(
									set.value().placement()
								)
							)
							.bigglobe_generateStructuresInArea(
								new SmartStructurePlacement.Context(
									this.locator(),
									context,
									set,
									context.columnSource().lookup(),
									area,
									true
								)
							)
						))
						.map(ChunkSortedStructurePieces::new)
						.collect(Collectors.groupingBy((ChunkSortedStructurePieces pieces) -> pieces.startWrapper.originalStructure()))
					);
				},
				STRUCTURE_EXECUTOR
			));
		}

		public CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>> getFiltered(Context context) {
			return this.filtered.getOrSetBlocking(() -> {
				return this.getUnfiltered(context).thenCompose((Map<Holder<Structure>, List<ChunkSortedStructurePieces>> unfiltered) -> {
					if (unfiltered.isEmpty()) return CompletableFuture.completedFuture(Collections.emptyMap());
					BoundingBox union = BoundingBox.encapsulatingBoxes(unfiltered.values().stream().flatMap(List<ChunkSortedStructurePieces>::stream).map((ChunkSortedStructurePieces pieces) -> pieces.startWrapper.box())::iterator).orElseThrow();
					BoundingBox centerArea = new BoundingBox(
						union.minX(),
						HeightLimitViewVersions.getMinY(context.height()),
						union.minZ(),
						union.maxX(),
						HeightLimitViewVersions.getMaxY(context.height()) - 1,
						union.maxZ()
					);
					Map<StructurePos, List<Holder<Structure>>> needed = (
						this.locator().allStructures().stream().collect(Grouper.groupingToList(
							Grouper.keysPerElement((Holder<Structure> structure) -> {
								int expansion = ((SizedStructure)(structure.value())).bigglobe_getMaxRadiusInBlocks();
								return StructurePos.intersectingArea(centerArea.inflatedBy(expansion, 0, expansion));
							}),
							Grouper.valueElement()
						))
					);
					@SuppressWarnings("unchecked") //generic array.
					CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>[] nearby = (
						needed
						.keySet()
						.stream()
						.map((StructurePos pos) -> this.locator().getCachesBulk(pos).getUnfiltered(context))
						.toArray(CompletableFuture[]::new)
					);
					return submitLater(
						() -> {
							SortedOverriders overriders = context.chunkGenerator().getOverriders();
							return (
								unfiltered
								.values()
								.stream()
								.flatMap(List<ChunkSortedStructurePieces>::stream)
								.filter((ChunkSortedStructurePieces pieces) -> {
									ScriptedColumnLookup lookup = context.columnSource().lookup();
									for (CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>> future : nearby) {
										for (List<ChunkSortedStructurePieces> intersectors : future.join().values()) {
											for (ChunkSortedStructurePieces intersector : intersectors) {
												if (intersector.equals(pieces)) continue;
												if (SortedStructurePieces.intersects(pieces, intersector)) {
													int priority = overriders.getCollisionPriority(lookup, pieces.startWrapper, intersector.startWrapper);
													if (priority < 0) {
														if (canLog(pieces.startWrapper.originalStructure())) {
															BigGlobeMod.LOGGER.info("Structure " + pieces + " did not spawn because it collided with a " + intersector + " and a collision overrider returned a negative priority.");
														}
														return false;
													}
													else if (priority == 0 && pieces.volume <= intersector.volume) {
														if (canLog(pieces.startWrapper.originalStructure())) {
															BigGlobeMod.LOGGER.info("Structure " + pieces + " did not spawn because it collided with a " + intersector + " and the other structure is bigger.");
														}
														return false;
													}
												}
											}
										}
									}
									return true;
								})
								.collect(Collectors.groupingBy((ChunkSortedStructurePieces pieces) -> (
									pieces.startWrapper.originalStructure()
								)))
							);
						},
						nearby
					);
				});
			});
		}

		public CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>> getIntersecting(Context context) {
			return this.intersecting.getOrSetBlocking(() -> {
				BoundingBox selfArea = this.pos.toArea(context.height());
				Map<StructurePos, List<Holder<Structure>>> needed = (
					this.locator().allStructures().stream().collect(Grouper.groupingToList(
						Grouper.keysPerElement((Holder<Structure> structure) -> {
							int expansion = ((SizedStructure)(structure.value())).bigglobe_getMaxRadiusInBlocks();
							return StructurePos.intersectingArea(selfArea.inflatedBy(expansion, 0, expansion));
						}),
						Grouper.valueElement()
					))
				);
				@SuppressWarnings("unchecked") //generic array.
				CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>[] nearby = (
					needed
					.keySet()
					.stream()
					.map((StructurePos pos) -> this.locator().getCachesBulk(pos).getFiltered(context))
					.toArray(CompletableFuture[]::new)
				);
				return submitLater(
					() -> (
						Arrays
						.stream(nearby)
						.map(CompletableFuture<Map<Holder<Structure>, List<ChunkSortedStructurePieces>>>::join)
						.flatMap((Map<Holder<Structure>, List<ChunkSortedStructurePieces>> map) -> map.values().stream())
						.flatMap(List<ChunkSortedStructurePieces>::stream)
						.filter((ChunkSortedStructurePieces pieces) -> pieces.startWrapper.box().intersects(selfArea))
						.collect(Collectors.groupingBy((ChunkSortedStructurePieces pieces) -> {
							return pieces.startWrapper.originalStructure();
						}))
					),
					nearby
				);
			});
		}
	}

	public static class ChunkSortedStructurePieces extends SortedStructurePieces {

		public ChunkSortedStructurePieces(StructureStartWrapper startWrapper) {
			List<StructurePiece> pieces = startWrapper.start().getPieces();
			Long2ObjectMap<List<StructurePiece>> buckets = new Long2ObjectOpenHashMap<>(pieces.size());
			int volume = volumeOf(startWrapper.start());
			int scale = Math.getExponent(Math.sqrt(((double)(volume)) / ((double)(pieces.size()))));
			for (StructurePiece piece : pieces) {
				BoundingBox box = piece.getBoundingBox();
				int minX = box.minX() >> scale;
				int minZ = box.minZ() >> scale;
				int maxX = box.maxX() >> scale;
				int maxZ = box.maxZ() >> scale;
				for (int z = minZ; z <= maxZ; z++) {
					for (int x = minX; x <= maxX; x++) {
						buckets.computeIfAbsent(
							ChunkPos.pack(x, z),
							(long packed) -> new ArrayList<>(4)
						)
						.add(piece);
					}
				}
			}
			super(startWrapper, buckets, volume, scale);
		}

		@Override
		public boolean intersects(BoundingBox box) {
			int minX = box.minX() >> this.scale;
			int minZ = box.minZ() >> this.scale;
			int maxX = box.maxX() >> this.scale;
			int maxZ = box.maxZ() >> this.scale;
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					List<StructurePiece> pieces = this.buckets.get(ChunkPos.pack(x, z));
					if (pieces != null) {
						for (int index = 0, size = pieces.size(); index < size; index++) {
							if (pieces.get(index).getBoundingBox().intersects(box)) return true;
						}
					}
				}
			}
			return false;
		}
	}
}