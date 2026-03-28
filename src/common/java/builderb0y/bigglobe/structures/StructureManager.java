package builderb0y.bigglobe.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.config.BigGlobeConfig.DataPackDebugging;
import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public abstract class StructureManager {

	public static record StructureGenerationParams(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
		ChunkGeneratorStructureState structurePlacementCalculator,
		RegistryAccess dynamicRegistries,
		RandomState noiseConfig,
		StructureTemplateManager structureTemplateManager,
		LevelHeightAccessor heightLimitView,
		ChunkPos chunkPos,
		boolean distantHorizons
	) {

		public StructureGenerationParams(
			BigGlobeScriptedChunkGenerator generator,
			ScriptedColumnLookup columns,
			ServerLevel world,
			ChunkPos chunkPos
		) {
			this(
				generator,
				columns,
				world.getChunkSource().getGeneratorState(),
				world.registryAccess(),
				world.getChunkSource().randomState(),
				world.getStructureManager(),
				world,
				chunkPos,
				columns.getHints().isLod()
			);
		}

		public BiomeSource biomeSource() {
			return this.generator.biome_source();
		}

		public long columnSeed() {
			return this.generator.columnSeed;
		}

		public Structure.GenerationContext toStructureContext(Predicate<Holder<Biome>> predicate) {
			return new Structure.GenerationContext(
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

	public static ResourceKey<Structure> structureKey(Structure structure) {
		return RegistryVersions.getRegistry(BigGlobeMod.getCurrentServer().registryAccess(), Registries.STRUCTURE).getResourceKey(structure).orElseThrow();
	}

	public static Identifier structureID(Structure structure) {
		return structureKey(structure).identifier();
	}

	public static String structureName(Structure structure) {
		return structureID(structure).toString();
	}

	public static boolean canLog(Structure structure) {
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.logStructureSpawning) {
			Pattern pattern = DataPackDebugging.structureLogFilterPattern;
			return pattern == null || pattern.matcher(structureName(structure)).find();
		}
		return false;
	}

	public static ResourceKey<Structure> structureKey(Holder<Structure> structure) {
		return UnregisteredObjectException.getKey(structure);
	}

	public static Identifier structureID(Holder<Structure> structure) {
		return structureKey(structure).identifier();
	}

	public static String structureName(Holder<Structure> structure) {
		return structureID(structure).toString();
	}

	public static boolean canLog(Holder<Structure> structure) {
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.logStructureSpawning) {
			Pattern pattern = DataPackDebugging.structureLogFilterPattern;
			return pattern == null || pattern.matcher(structureName(structure)).find();
		}
		return false;
	}

	public abstract FinalStructures getIntersectingStructures(StructureGenerationParams params);

	public abstract ScriptStructures[] computeRelevantStructuresForOverriders(StructureGenerationParams params, ColumnValueOverridersWithRadiusCache overriders);

	public abstract FinalStructures getFinalStructures(StructureGenerationParams params);

	public abstract StructureManager copy();

	public static record StructureKey(int chunkX, int chunkZ, Holder<StructureSet> set) {

		public ChunkPos chunkPos() {
			return new ChunkPos(this.chunkX, this.chunkZ);
		}
	}

	public static class SectionSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> {

		public final Holder<StructureSet> set;
		public final StructureStartWrapper startWrapper;
		public final int volume, scale;

		public SectionSortedStructurePieces(Holder<StructureSet> set) {
			this.set = set;
			this.startWrapper = null;
			this.volume = 0;
			this.scale = 0;
		}

		public SectionSortedStructurePieces(Holder<StructureSet> set, StructureStartWrapper startWrapper) {
			super(startWrapper.start().getPieces().size());
			this.set = set;
			this.startWrapper = startWrapper;
			this.volume = volumeOf(startWrapper.start());
			List<StructurePiece> children = startWrapper.start().getPieces();
			this.scale = Math.getExponent(Math.cbrt(((double)(this.volume)) / ((double)(children.size()))));
			for (StructurePiece piece : children) {
				BoundingBox box = piece.getBoundingBox();
				int minX = box.minX() >> this.scale;
				int minY = box.minY() >> this.scale;
				int minZ = box.minZ() >> this.scale;
				int maxX = box.maxX() >> this.scale;
				int maxY = box.maxY() >> this.scale;
				int maxZ = box.maxZ() >> this.scale;
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
		}

		public boolean intersects(StructureStart start) {
			for (StructurePiece child : start.getPieces()) {
				if (this.intersects(child)) return true;
			}
			return false;
		}

		public static boolean intersects(SectionSortedStructurePieces pieces1, SectionSortedStructurePieces pieces2) {
			StructureStart start1 = pieces1.startWrapper.start();
			StructureStart start2 = pieces2.startWrapper.start();
			return start1.getBoundingBox().intersects(start2.getBoundingBox()) && (start1.getPieces().size() < start2.getPieces().size() ? pieces2.intersects(start1) : pieces1.intersects(start2));
		}

		public static int volumeOf(StructureStart start) {
			if (!start.isValid()) return 0;
			BoundingBox box = start.getBoundingBox();
			int minX = box.minX();
			int minY = box.minY();
			int minZ = box.minZ();
			int maxX = box.maxX();
			int maxY = box.maxY();
			int maxZ = box.maxZ();
			return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		}

		public boolean intersects(StructurePiece piece) {
			BoundingBox box = piece.getBoundingBox();
			int minX = box.minX() >> this.scale;
			int minY = box.minY() >> this.scale;
			int minZ = box.minZ() >> this.scale;
			int maxX = box.maxX() >> this.scale;
			int maxY = box.maxY() >> this.scale;
			int maxZ = box.maxZ() >> this.scale;
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

	public static class FinalStructures extends ObjectArrayList<StructureStart> {

		public FinalStructures() {
		}

		public FinalStructures(int capacity) {
			super(capacity);
		}
	}
}