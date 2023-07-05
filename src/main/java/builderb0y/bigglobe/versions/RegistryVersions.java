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
import net.minecraft.sound.SoundEvent;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.structure.StructureType;

public class RegistryVersions {

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

	@SuppressWarnings("unchecked")
	public static <T> RegistryKey<Registry<T>> getRegistryKey(Registry<T> registry) {
		return (RegistryKey<Registry<T>>)(registry.getKey());
	}
}