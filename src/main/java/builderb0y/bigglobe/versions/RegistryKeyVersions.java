package builderb0y.bigglobe.versions;

import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

#if MC_VERSION > MC_1_19_2
	import net.minecraft.registry.RegistryKeys;
#endif

public class RegistryKeyVersions {

	#if MC_VERSION <= MC_1_19_2
		public static RegistryKey<Registry<Block                  >> block                 () { return Registry.BLOCK_KEY                   ; }
		public static RegistryKey<Registry<Item                   >> item                  () { return Registry.ITEM_KEY                    ; }
		public static RegistryKey<Registry<Fluid                  >> fluid                 () { return Registry.FLUID_KEY                   ; }
		public static RegistryKey<Registry<DimensionType          >> dimensionType         () { return Registry.DIMENSION_TYPE_KEY          ; }
		public static RegistryKey<Registry<ConfiguredCarver    <?>>> configuredCarver      () { return Registry.CONFIGURED_CARVER_KEY       ; }
		public static RegistryKey<Registry<ConfiguredFeature<?, ?>>> configuredFeature     () { return Registry.CONFIGURED_FEATURE_KEY      ; }
		public static RegistryKey<Registry<PlacedFeature          >> placedFeature         () { return Registry.PLACED_FEATURE_KEY          ; }
		public static RegistryKey<Registry<Structure              >> structure             () { return Registry.STRUCTURE_KEY               ; }
		public static RegistryKey<Registry<StructureSet           >> structureSet          () { return Registry.STRUCTURE_SET_KEY           ; }
		public static RegistryKey<Registry<StructureProcessorList >> processorList         () { return Registry.STRUCTURE_PROCESSOR_LIST_KEY; }
		public static RegistryKey<Registry<StructurePool          >> templatePool          () { return Registry.STRUCTURE_POOL_KEY          ; }
		public static RegistryKey<Registry<Biome                  >> biome                 () { return Registry.BIOME_KEY                   ; }
		public static RegistryKey<Registry<DensityFunction        >> densityFunction       () { return Registry.DENSITY_FUNCTION_KEY        ; }
		public static RegistryKey<Registry<ChunkGeneratorSettings >> chunkGeneratorSettings() { return Registry.CHUNK_GENERATOR_SETTINGS_KEY; }
		public static RegistryKey<Registry<WorldPreset            >> worldPreset           () { return Registry.WORLD_PRESET_KEY            ; }
		public static RegistryKey<Registry<DimensionOptions       >> dimension             () { return Registry.DIMENSION_KEY               ; }
		public static RegistryKey<Registry<World                  >> world                 () { return Registry.WORLD_KEY                   ; }
		public static RegistryKey<Registry<StructureType<?>       >> structureType         () { return Registry.STRUCTURE_TYPE_KEY          ; }
		public static RegistryKey<Registry<StructurePieceType     >> structurePieceType    () { return Registry.STRUCTURE_PIECE_KEY         ; }
	#else
		public static RegistryKey<Registry<Block                  >> block                 () { return RegistryKeys.BLOCK                   ; }
		public static RegistryKey<Registry<Item                   >> item                  () { return RegistryKeys.ITEM                    ; }
		public static RegistryKey<Registry<Fluid                  >> fluid                 () { return RegistryKeys.FLUID                   ; }
		public static RegistryKey<Registry<DimensionType          >> dimensionType         () { return RegistryKeys.DIMENSION_TYPE          ; }
		public static RegistryKey<Registry<ConfiguredCarver    <?>>> configuredCarver      () { return RegistryKeys.CONFIGURED_CARVER       ; }
		public static RegistryKey<Registry<ConfiguredFeature<?, ?>>> configuredFeature     () { return RegistryKeys.CONFIGURED_FEATURE      ; }
		public static RegistryKey<Registry<PlacedFeature          >> placedFeature         () { return RegistryKeys.PLACED_FEATURE          ; }
		public static RegistryKey<Registry<Structure              >> structure             () { return RegistryKeys.STRUCTURE               ; }
		public static RegistryKey<Registry<StructureSet           >> structureSet          () { return RegistryKeys.STRUCTURE_SET           ; }
		public static RegistryKey<Registry<StructureProcessorList >> processorList         () { return RegistryKeys.PROCESSOR_LIST          ; }
		public static RegistryKey<Registry<StructurePool          >> templatePool          () { return RegistryKeys.TEMPLATE_POOL           ; }
		public static RegistryKey<Registry<Biome                  >> biome                 () { return RegistryKeys.BIOME                   ; }
		public static RegistryKey<Registry<DensityFunction        >> densityFunction       () { return RegistryKeys.DENSITY_FUNCTION        ; }
		public static RegistryKey<Registry<ChunkGeneratorSettings >> chunkGeneratorSettings() { return RegistryKeys.CHUNK_GENERATOR_SETTINGS; }
		public static RegistryKey<Registry<WorldPreset            >> worldPreset           () { return RegistryKeys.WORLD_PRESET            ; }
		public static RegistryKey<Registry<DimensionOptions       >> dimension             () { return RegistryKeys.DIMENSION               ; }
		public static RegistryKey<Registry<World                  >> world                 () { return RegistryKeys.WORLD                   ; }
		public static RegistryKey<Registry<StructureType<?>       >> structureType         () { return RegistryKeys.STRUCTURE_TYPE          ; }
		public static RegistryKey<Registry<StructurePieceType     >> structurePieceType    () { return RegistryKeys.STRUCTURE_PIECE         ; }
	#endif

	#if MC_VERSION >= MC_1_20_5
		public static RegistryKey<Registry<LootTable>>    lootTable   () { return RegistryKeys.LOOT_TABLE   ; }
		public static RegistryKey<Registry<StatusEffect>> statusEffect() { return RegistryKeys.STATUS_EFFECT; }
	#endif
}