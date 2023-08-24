package builderb0y.bigglobe.versions;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.structure.StructureType;

public class RegistryVersions {

	#if MC_VERSION <= MC_1_19_2
		public static DefaultedRegistry<Block                          > block                 () { return Registry.BLOCK               ; }
		public static Registry         <BlockEntityType<?>             > blockEntityType       () { return Registry.BLOCK_ENTITY_TYPE   ; }
		public static DefaultedRegistry<Item                           > item                  () { return Registry.ITEM                ; }
		public static DefaultedRegistry<Fluid                          > fluid                 () { return Registry.FLUID               ; }
		public static DefaultedRegistry<EntityType<?>                  > entityType            () { return Registry.ENTITY_TYPE         ; }
		public static DefaultedRegistry<Potion                         > potion                () { return Registry.POTION              ; }
		public static Registry         <Feature<?>                     > feature               () { return Registry.FEATURE             ; }
		public static Registry         <Codec<? extends ChunkGenerator>> chunkGenerator        () { return Registry.CHUNK_GENERATOR     ; }
		public static Registry         <LootPoolEntryType              > lootPoolEntryType     () { return Registry.LOOT_POOL_ENTRY_TYPE; }
		public static Registry         <LootFunctionType               > lootFunctionType      () { return Registry.LOOT_FUNCTION_TYPE  ; }
		public static Registry         <ParticleType<?>                > particleType          () { return Registry.PARTICLE_TYPE       ; }
		public static Registry         <RecipeSerializer<?>            > recipeSerializer      () { return Registry.RECIPE_SERIALIZER   ; }
		public static Registry         <StructureType<?>               > structureType         () { return Registry.STRUCTURE_TYPE      ; }
		public static Registry         <StructurePieceType             > structurePieceType    () { return Registry.STRUCTURE_PIECE     ; }
		public static Registry         <StructurePlacementType<?>      > structurePlacementType() { return Registry.STRUCTURE_PLACEMENT ; }
		public static Registry         <SoundEvent                     > soundEvent            () { return Registry.SOUND_EVENT         ; }
	#else
		public static DefaultedRegistry<Block                          > block                 () { return Registries.BLOCK               ; }
		public static Registry         <BlockEntityType<?>             > blockEntityType       () { return Registries.BLOCK_ENTITY_TYPE   ; }
		public static DefaultedRegistry<Item                           > item                  () { return Registries.ITEM                ; }
		public static DefaultedRegistry<Fluid                          > fluid                 () { return Registries.FLUID               ; }
		public static DefaultedRegistry<EntityType<?>                  > entityType            () { return Registries.ENTITY_TYPE         ; }
		public static DefaultedRegistry<Potion                         > potion                () { return Registries.POTION              ; }
		public static Registry         <Feature<?>                     > feature               () { return Registries.FEATURE             ; }
		public static Registry         <Codec<? extends ChunkGenerator>> chunkGenerator        () { return Registries.CHUNK_GENERATOR     ; }
		public static Registry         <LootPoolEntryType              > lootPoolEntryType     () { return Registries.LOOT_POOL_ENTRY_TYPE; }
		public static Registry         <LootFunctionType               > lootFunctionType      () { return Registries.LOOT_FUNCTION_TYPE  ; }
		public static Registry         <ParticleType<?>                > particleType          () { return Registries.PARTICLE_TYPE       ; }
		public static Registry         <RecipeSerializer<?>            > recipeSerializer      () { return Registries.RECIPE_SERIALIZER   ; }
		public static Registry         <StructureType<?>               > structureType         () { return Registries.STRUCTURE_TYPE      ; }
		public static Registry         <StructurePieceType             > structurePieceType    () { return Registries.STRUCTURE_PIECE     ; }
		public static Registry         <StructurePlacementType<?>      > structurePlacementType() { return Registries.STRUCTURE_PLACEMENT ; }
		public static Registry         <SoundEvent                     > soundEvent            () { return Registries.SOUND_EVENT         ; }
	#endif

	@SuppressWarnings("unchecked")
	public static <T> RegistryKey<Registry<T>> getRegistryKey(Registry<T> registry) {
		return (RegistryKey<Registry<T>>)(registry.getKey());
	}

	#if MC_VERSION > MC_1_19_2
		@SuppressWarnings("unchecked")
		public static <T> RegistryKey<Registry<T>> getRegistryKey(RegistryWrapper.Impl<T> registry) {
			return (RegistryKey<Registry<T>>)(registry.getRegistryKey());
		}
	#endif
}