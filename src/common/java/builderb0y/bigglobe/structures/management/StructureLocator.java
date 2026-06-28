package builderb0y.bigglobe.structures.management;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.config.BigGlobeConfig.DataPackDebugging;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.StructureLocator.WhatToSearchFor.ManyStructuresOneBox;
import builderb0y.bigglobe.util.Streamable;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;

public abstract class StructureLocator {

	public abstract Stream<StructureStartWrapper> getStructuresIntersecting(Params params);

	public abstract @Nullable WeightedList<SpawnerData> getMobSpawns(Context context, BlockPos blockPos, MobCategory group);

	public abstract Stream<StructureStartWrapper> getStructuresInside(Params params);

	public abstract Stream<StructureStartWrapper> getStructuresNearby(Params params, BlockPos center);

	public abstract boolean maybeHasBiomes(BiomeParams params);

	public abstract Streamable<Holder<Structure>> allStructures();

	public static <T extends BaseStream<?, T>> T maybeParallel(T stream) {
		return BigGlobeConfig.INSTANCE.get().c2meIntegration.multiThreadedStructures() ? stream.parallel() : stream.sequential();
	}

	public static Registry<Structure> structureRegistry() {
		return RegistryVersions.getRegistry(BigGlobeMod.getCurrentServer().registryAccess(), Registries.STRUCTURE);
	}

	public static Registry<Structure> structureRegistry(LevelReader level) {
		return RegistryVersions.getRegistry(level.registryAccess(), Registries.STRUCTURE);
	}

	public static Registry<Structure> structureRegistry(RegistryAccess registries) {
		return RegistryVersions.getRegistry(registries, Registries.STRUCTURE);
	}

	public static Holder<Structure> toHolder(Structure structure) {
		return RegistryVersions.getEntry(structureRegistry(), structure);
	}

	public static Holder<Structure> toHolder(Registry<Structure> registry, Structure structure) {
		return RegistryVersions.getEntry(registry, structure);
	}

	public static Holder<Structure> toHolder(RegistryAccess registries, Structure structure) {
		return RegistryVersions.getEntry(structureRegistry(registries), structure);
	}

	public static Holder<Structure> toHolder(LevelReader world, Structure structure) {
		return RegistryVersions.getEntry(structureRegistry(world), structure);
	}

	public static ResourceKey<Structure> structureKey(Structure structure) {
		return structureRegistry().getResourceKey(structure).orElseThrow();
	}

	public static Identifier structureID(Structure structure) {
		return structureKey(structure).identifier();
	}

	public static String structureName(Structure structure) {
		return structureID(structure).toString();
	}

	public static boolean canLog(Structure structure) {
		return canLog(structureName(structure));
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
		return canLog(structureName(structure));
	}

	public static boolean canLog(String structureName) {
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.logStructureSpawning) {
			Pattern pattern = DataPackDebugging.structureLogFilterPattern;
			return pattern == null || pattern.matcher(structureName).find();
		}
		return false;
	}

	public static abstract sealed class WhatToSearchFor {

		public final Streamable<Holder<Structure>> structures;

		public WhatToSearchFor(Streamable<Holder<Structure>> structures) {
			this.structures = structures;
		}

		public Stream<Holder<Structure>> streamStructures() {
			return this.structures.stream();
		}

		public abstract BoundingBox getAreaFor(Holder<Structure> structure);

		public abstract WhatToSearchFor filter(Streamable<Holder<Structure>> structures);

		public static non-sealed class ManyStructuresOneBox extends WhatToSearchFor {

			public final BoundingBox box;

			public ManyStructuresOneBox(Streamable<Holder<Structure>> structures, BoundingBox box) {
				super(structures);
				this.box = box;
			}

			@Override
			public WhatToSearchFor filter(Streamable<Holder<Structure>> structures) {
				return new ManyStructuresOneBox(structures, this.box);
			}

			@Override
			public BoundingBox getAreaFor(Holder<Structure> structure) {
				return this.box;
			}
		}

		public static abstract non-sealed class ManyStructuresManyBoxes extends WhatToSearchFor {

			public ManyStructuresManyBoxes(Streamable<Holder<Structure>> structures) {
				super(structures);
			}
		}
	}

	public static record Context(
		BigGlobeScriptedChunkGenerator chunkGenerator,
		ConfiguredColumnFactory columnSource,
		ChunkGeneratorStructureState structureState,
		RegistryAccess dynamicRegistries,
		StructureTemplateManager structureTemplateManager,
		LevelHeightAccessor height
	) {

		public Context(
			BigGlobeScriptedChunkGenerator chunkGenerator,
			ConfiguredColumnFactory columnSource,
			ServerLevel world
		) {
			this(
				chunkGenerator,
				columnSource,
				world.getChunkSource().getGeneratorState(),
				world.registryAccess(),
				world.getStructureManager(),
				world
			);
		}
	}

	public static record Params(
		Context context,
		WhatToSearchFor whatToSearchFor
	) {

		public Params(
			BigGlobeScriptedChunkGenerator chunkGenerator,
			ConfiguredColumnFactory columnSource,
			ServerLevel world,
			WhatToSearchFor whatToSearchFor
		) {
			this(new Context(chunkGenerator, columnSource, world), whatToSearchFor);
		}

		public BigGlobeScriptedChunkGenerator chunkGenerator          () { return this.context.chunkGenerator          ; }
		public ConfiguredColumnFactory        columnSource            () { return this.context.columnSource            ; }
		public ChunkGeneratorStructureState   structureState          () { return this.context.structureState          ; }
		public RegistryAccess                 dynamicRegistries       () { return this.context.dynamicRegistries       ; }
		public StructureTemplateManager       structureTemplateManager() { return this.context.structureTemplateManager; }
		public LevelHeightAccessor            height                  () { return this.context.height                  ; }

		public Params searchFor(WhatToSearchFor whatToSearchFor) {
			return new Params(
				this.context,
				whatToSearchFor
			);
		}

		public Params searchFor(Streamable<Holder<Structure>> structures) {
			return this.searchFor(this.whatToSearchFor.filter(structures));
		}

		public Params searchIn(BoundingBox area) {
			return this.searchFor(new ManyStructuresOneBox(this.whatToSearchFor.structures, area));
		}
	}

	public static record BiomeParams(
		Context context,
		BoundingBox area,
		Predicate<Holder<Biome>> predicate
	) {

		public BigGlobeScriptedChunkGenerator chunkGenerator          () { return this.context.chunkGenerator          ; }
		public ConfiguredColumnFactory        columnSource            () { return this.context.columnSource            ; }
		public ChunkGeneratorStructureState   structureState          () { return this.context.structureState          ; }
		public RegistryAccess                 dynamicRegistries       () { return this.context.dynamicRegistries       ; }
		public StructureTemplateManager       structureTemplateManager() { return this.context.structureTemplateManager; }
		public LevelHeightAccessor            height                  () { return this.context.height                  ; }
	}

	public static abstract class SortedStructurePieces {

		public final StructureStartWrapper startWrapper;
		public final Long2ObjectMap<List<StructurePiece>> buckets;
		public final int volume, scale;

		public SortedStructurePieces(
			StructureStartWrapper startWrapper,
			Long2ObjectMap<List<StructurePiece>> buckets,
			int volume,
			int scale
		) {
			this.startWrapper = startWrapper;
			this.buckets = buckets;
			this.volume = volume;
			this.scale = scale;
		}

		public static boolean intersects(SortedStructurePieces pieces1, SortedStructurePieces pieces2) {
			StructureStart start1 = pieces1.startWrapper.start();
			StructureStart start2 = pieces2.startWrapper.start();
			return start1.getBoundingBox().intersects(start2.getBoundingBox()) && (
				start1.getPieces().size() > start2.getPieces().size()
				? pieces1.intersects(start2)
				: pieces2.intersects(start1)
			);
		}

		public boolean intersects(StructureStart start) {
			for (StructurePiece piece : start.getPieces()) {
				if (this.intersects(piece.getBoundingBox())) return true;
			}
			return false;
		}

		public abstract boolean intersects(BoundingBox box);

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

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof SortedStructurePieces that)) return false;
			StructureStart
				start1 = this.startWrapper.start(),
				start2 = that.startWrapper.start();
			if (start1 == null) return start2 == null;
			if (start2 == null) return false;
			if (start1.getStructure() != start2.getStructure()) return false;
			if (!start1.getBoundingBox().equals(start2.getBoundingBox())) return false;
			return start1.getPieces().size() == start2.getPieces().size();
		}

		@Override
		public String toString() {
			return this.startWrapper.originalID() + " at " + this.startWrapper.box();
		}
	}
}