package builderb0y.bigglobe.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public abstract class StructureManager {

	public static record StructureGenerationParams(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
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
			ServerWorld world,
			ChunkPos chunkPos
		) {
			this(
				generator,
				columns,
				world.getChunkManager().getStructurePlacementCalculator(),
				world.getRegistryManager(),
				world.getChunkManager().getNoiseConfig(),
				world.getStructureTemplateManager(),
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

	public abstract FinalStructures getIntersectingStructures(StructureGenerationParams params);

	public abstract ScriptStructures[] computeRelevantStructuresForOverriders(StructureGenerationParams params, ColumnValueOverridersWithRadiusCache overriders);

	public abstract FinalStructures getFinalStructures(StructureGenerationParams params);

	public abstract StructureManager copy();

	public static record StructureKey(int chunkX, int chunkZ, RegistryEntry<StructureSet> set) {

		public ChunkPos chunkPos() {
			return new ChunkPos(this.chunkX, this.chunkZ);
		}
	}

	public static class SectionSortedStructurePieces extends Long2ObjectOpenHashMap<List<StructurePiece>> {

		public final RegistryEntry<StructureSet> set;
		public final StructureStartWrapper startWrapper;
		public final int volume, scale;

		public SectionSortedStructurePieces(RegistryEntry<StructureSet> set) {
			this.set = set;
			this.startWrapper = null;
			this.volume = 0;
			this.scale = 0;
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
			return start1.getBoundingBox().intersects(start2.getBoundingBox()) && (start1.getChildren().size() < start2.getChildren().size() ? pieces2.intersects(start1) : pieces1.intersects(start2));
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

	public static class FinalStructures extends ObjectArrayList<StructureStart> {

		public FinalStructures() {}

		public FinalStructures(int capacity) {
			super(capacity);
		}
	}
}