package builderb0y.bigglobe.versions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.loot.entry.LootPoolEntryType;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.particle.ParticleType;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.structure.StructureType;

public class RegistryVersions {

	public static Registry         <Block                             > block                 () { return Registries.BLOCK               ; }
	public static Registry         <BlockEntityType<?>                > blockEntityType       () { return Registries.BLOCK_ENTITY_TYPE   ; }
	public static Registry         <Item                              > item                  () { return Registries.ITEM                ; }
	public static Registry         <Fluid                             > fluid                 () { return Registries.FLUID               ; }
	public static Registry         <EntityType<?>                     > entityType            () { return Registries.ENTITY_TYPE         ; }
	public static Registry         <Potion                            > potion                () { return Registries.POTION              ; }
	public static Registry         <Feature<?>                        > feature               () { return Registries.FEATURE             ; }
	public static Registry         <LootPoolEntryType                 > lootPoolEntryType     () { return Registries.LOOT_POOL_ENTRY_TYPE; }
	public static Registry         <ParticleType<?>                   > particleType          () { return Registries.PARTICLE_TYPE       ; }
	public static Registry         <RecipeSerializer<?>               > recipeSerializer      () { return Registries.RECIPE_SERIALIZER   ; }
	public static Registry         <StructureType<?>                  > structureType         () { return Registries.STRUCTURE_TYPE      ; }
	public static Registry         <StructurePieceType                > structurePieceType    () { return Registries.STRUCTURE_PIECE     ; }
	public static Registry         <StructurePlacementType<?>         > structurePlacementType() { return Registries.STRUCTURE_PLACEMENT ; }
	public static Registry         <SoundEvent                        > soundEvent            () { return Registries.SOUND_EVENT         ; }
	public static Registry         <ArmorMaterial                     > armorMaterial         () { return Registries.ARMOR_MATERIAL      ; }

	#if MC_VERSION >= MC_1_20_5
		public static Registry     <MapCodec<? extends ChunkGenerator>> chunkGenerator        () { return Registries.CHUNK_GENERATOR     ; }
		public static Registry     <MapCodec<? extends BiomeSource   >> biomeSource           () { return Registries.BIOME_SOURCE        ; }
		public static Registry     <LootFunctionType<?>               > lootFunctionType      () { return Registries.LOOT_FUNCTION_TYPE  ; }
	#else
		public static Registry     <Codec<? extends ChunkGenerator>   > chunkGenerator        () { return Registries.CHUNK_GENERATOR     ; }
		public static Registry     <Codec<? extends BiomeSource   >   > biomeSource           () { return Registries.BIOME_SOURCE        ; }
		public static Registry     <LootFunctionType                  > lootFunctionType      () { return Registries.LOOT_FUNCTION_TYPE  ; }
	#endif

	#if MC_VERSION >= MC_1_20_3
		public static Registry     <StatusEffect                      > statusEffect          () { return Registries.STATUS_EFFECT       ; }
	#endif

	@SuppressWarnings("unchecked")
	public static <T> RegistryKey<Registry<T>> getRegistryKey(Registry<T> registry) {
		return (RegistryKey<Registry<T>>)(registry.getKey());
	}

	@SuppressWarnings("unchecked")
	public static <T> RegistryKey<Registry<T>> getRegistryKey(RegistryWrapper.Impl<T> registry) {
		return (RegistryKey<Registry<T>>)(registry.getRegistryKey());
	}
}