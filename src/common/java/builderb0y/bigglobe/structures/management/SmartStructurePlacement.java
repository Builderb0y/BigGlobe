package builderb0y.bigglobe.structures.management;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumnLookup;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.StructureOverrider;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.structures.management.StructureLocator.BiomeParams;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;

public interface SmartStructurePlacement {

	public abstract Stream<StructureStartWrapper> bigglobe_generateStructuresInArea(Context context);

	public static record Context(
		StructureLocator caller,
		StructureLocator.Context locatorContext,
		Holder<StructureSet> structureSet,
		ScriptedColumnLookup columns,
		BoundingBox area,
		boolean strict
	) {

		public static final Set<Class<? extends Structure>> eagerStructures = Collections.synchronizedSet(new HashSet<>());

		public BigGlobeScriptedChunkGenerator chunkGenerator          () { return this.locatorContext.chunkGenerator()              ; }
		public ConfiguredColumnFactory        columnSource            () { return this.locatorContext.columnSource()                ; }
		public ChunkGeneratorStructureState   structureState          () { return this.locatorContext.structureState()              ; }
		public RegistryAccess                 dynamicRegistries       () { return this.locatorContext.dynamicRegistries()           ; }
		public StructureTemplateManager       structureTemplateManager() { return this.locatorContext.structureTemplateManager()    ; }
		public LevelHeightAccessor            height                  () { return this.locatorContext.height()                      ; }
		public RandomState                    randomState             () { return this.locatorContext.structureState().randomState(); }
		public long                           hashedWorldSeed         () { return this.locatorContext.chunkGenerator().columnSeed   ; }
		public boolean                        distantHorizons         () { return this.columns.getHints().isLod()                   ; }

		public GenerationContext toStructureContext(ChunkPos chunkPos, Predicate<Holder<Biome>> predicate) {
			return new Structure.GenerationContext(
				this.dynamicRegistries(),
				this.chunkGenerator(),
				this.chunkGenerator().biome_source(),
				this.randomState(),
				this.structureTemplateManager(),
				this.hashedWorldSeed(),
				chunkPos,
				this.height(),
				predicate
			);
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

		public @Nullable StructureStartWrapper createRandomFromSetAt(ChunkPos chunkPos) {
			if (this.strict && !this.intersects(chunkPos)) return null;
			long chunkSeed = Permuter.permute(this.hashedWorldSeed() ^ 0x767DB826EDD5532EL, chunkPos.x(), chunkPos.z(), UnregisteredObjectException.getID(this.structureSet).hashCode());
			Permuter structureChooser = new Permuter(chunkSeed);
			List<StructureSelectionEntry> possibilities = new ArrayList<>(this.structureSet.value().structures());
			int totalWeight = getTotalWeight(possibilities);
			while (!possibilities.isEmpty()) {
				int index = getRandomIndex(possibilities, totalWeight, structureChooser);
				StructureSelectionEntry entry = possibilities.get(index);
				StructureStartWrapper structure = this.createIn(chunkPos, entry.structure(), entry.structure().value().biomes()::contains);
				if (structure != null) {
					if (this.strict && !this.area.isInside(structure.pos())) {
						return null;
					}
					return structure;
				}
				else {
					if (index == possibilities.size() - 1) {
						possibilities.remove(index);
					}
					else {
						possibilities.set(index, possibilities.removeLast());
					}
					totalWeight -= entry.weight();
				}
			}
			return null;
		}

		public static PiecesContainer unwrap(Structure structure, GenerationStub stub) {
			return stub.generator().map(
				(Consumer<StructurePiecesBuilder> generator) -> {
					StructurePiecesBuilder builder = new StructurePiecesBuilder();
					generator.accept(builder);
					return builder.build();
				},
				(StructurePiecesBuilder builder) -> {
					Structure root = structure;
					while (root instanceof DelegatingStructure delegating) {
						root = delegating.delegate().value();
					}
					if (eagerStructures.add(root.getClass())) {
						BigGlobeMod.LOGGER.warn(root.getClass() + " does not generate pieces lazily! This will likely cause performance issues!");
					}
					return builder.build();
				}
			);
		}

		public @Nullable StructureStartWrapper createIn(ChunkPos chunkPos, Holder<Structure> structure, Predicate<Holder<Biome>> validBiomes) {
			return this.createIn(chunkPos, new StructureEntry(structure), validBiomes, true);
		}

		public boolean intersects(ChunkPos chunkPos) {
			return (
				chunkPos.getMinBlockX() <= this.area.maxX() &&
				HeightLimitViewVersions.getMinY(this.height()) <= this.area.maxY() &&
				chunkPos.getMinBlockZ() <= this.area.maxZ() &&
				chunkPos.getMaxBlockX() >= this.area.minX() &&
				HeightLimitViewVersions.getMaxY(this.height()) - 1 >= this.area.minY() &&
				chunkPos.getMaxBlockZ() >= this.area.minZ()
			);
		}

		public @Nullable StructureStartWrapper createIn(ChunkPos chunkPos, StructureEntry structure, Predicate<Holder<Biome>> validBiomes, boolean runOverriders) {
			if (this.strict && !this.intersects(chunkPos)) return null;
			if (
				validBiomes != Predicates.<Holder<Biome>>alwaysTrue()
				&& !this.caller.maybeHasBiomes(
					new BiomeParams(
						this.locatorContext,
						WorldUtil.chunkBox(chunkPos, this.height()),
						validBiomes
					)
				)
			) {
				if (StructureLocator.canLog(structure.entry)) {
					BigGlobeMod.LOGGER.info(
						"Structure " +
						structure.id() +
						" did not spawn at [" +
						(chunkPos.x() << 4) +
						", " +
						(chunkPos.z() << 4) +
						"] because a suitable biome was not found nearby."
					);
				}
				return null;
			}
			Optional<GenerationStub> optional = structure.entry.value().findValidGenerationPoint(this.toStructureContext(chunkPos, Predicates.alwaysTrue()));
			if (optional.isEmpty()) {
				if (StructureLocator.canLog(structure.entry)) {
					BigGlobeMod.LOGGER.info(
						"Structure " +
						structure.id() +
						" did not spawn at [" +
						(chunkPos.x() << 4) +
						", " +
						(chunkPos.z() << 4) +
						"] because the structure itself decided not to."
					);
				}
				return null;
			}
			GenerationStub stub = optional.get();
			if (this.strict && !this.area.isInside(stub.position())) {
				return null;
			}
			PiecesContainer pieces = unwrap(structure.entry.value(), stub);
			if (pieces.isEmpty()) {
				if (StructureLocator.canLog(structure.entry)) {
					BigGlobeMod.LOGGER.info(
						"Structure " +
						structure.id() +
						" did not spawn at [" +
						(chunkPos.x() << 4) +
						", " +
						(chunkPos.z() << 4) +
						"] because the structure had no pieces."
					);
				}
				return null;
			}
			StructureStartWrapper result = StructureStartWrapper.of(
				structure.entry,
				new StructureStart(
					structure.entry.value(),
					chunkPos,
					0,
					pieces
				),
				stub.position()
			);
			if (runOverriders) {
				if (!this.override(result)) {
					return null;
				}
				BlockPos pos = result.clampedPos();
				Holder<Biome> biome = (
					this
					.locatorContext
					.chunkGenerator()
					.biome_source()
					.getNoiseBiome(
						pos.getX() >> 2,
						pos.getY() >> 2,
						pos.getZ() >> 2,
						this.randomState().sampler()
					)
				);
				if (!validBiomes.test(biome)) {
					if (StructureLocator.canLog(result.originalStructure())) {
						BigGlobeMod.LOGGER.info(
							"Structure " +
							result.originalID() +
							" did not spawn at " +
							pos +
							" because the biome at this location (" +
							UnregisteredObjectException.getID(biome) +
							") is not in the structure's biome list."
						);
					}
					return null;
				}
			}
			if (StructureLocator.canLog(structure.entry)) {
				BigGlobeMod.LOGGER.info("Structure " + result + " passed initial checks (though it *might* collide with something later).");
			}
			return result;
		}

		public boolean override(StructureStartWrapper start) {
			Permuter random = new Permuter(0L);
			for (Holder<StructureOverrider.Entry> overrider : this.chunkGenerator().getOverriders().structures) {
				if (
					!overrider.value().script().override(
						this.columns,
						start,
						random.withSeed(
							Permuter.permute(
								this.chunkGenerator().columnSeed ^ 0x8B3236675A8E72DDL,
								UnregisteredObjectException.getID(overrider).hashCode()
							)
						),
						this.chunkGenerator().columnSeed
					)
				) {
					if (StructureLocator.canLog(start.originalStructure())) {
						BigGlobeMod.LOGGER.info(
							"Structure " +
							StructureLocator.structureName(start.originalStructure()) +
							" did not spawn at " +
							start.start().getBoundingBox().getCenter() +
							" because overrider " +
							overrider.unwrapKey().orElseThrow().identifier() +
							" said no."
						);
					}
					return false;
				}
			}
			return true;
		}

		public void checkStructure(Holder<Structure> structure) {
			for (StructureSelectionEntry entry : this.structureSet.value().structures()) {
				if (entry.structure() == structure) return;
			}
			throw new IllegalArgumentException("Script attempted to place " + StructureLocator.structureName(structure) + " but that structure is not declared in the structure set.");
		}

		public @Nullable StructureStartWrapper createNear(BlockPos pos, StructureEntry structure) {
			this.checkStructure(structure.entry);
			if (this.strict && !this.area.isInside(pos)) return null;
			return this.createIn(ChunkPos.containing(pos), structure, Predicates.alwaysTrue(), false);
		}

		public @Nullable StructureStartWrapper createAt(BlockPos pos, StructureEntry structure) {
			StructureStartWrapper result = this.createNear(pos, structure);
			if (result != null && !result.pos().equals(pos)) result.move(
				pos.getX() - result.pos().getX(),
				pos.getY() - result.pos().getY(),
				pos.getZ() - result.pos().getZ()
			);
			return result;
		}
	}
}