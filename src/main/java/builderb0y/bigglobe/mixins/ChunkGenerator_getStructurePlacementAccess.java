package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

/**
used by big globe's implementation of ChunkGenerator.locateStructure().
the vanilla one appears optimized, but is hard-coded for vanilla structure placements.
I am overriding this to work with big globe's structure placements.
*/
@Mixin(ChunkGenerator.class)
public interface ChunkGenerator_getStructurePlacementAccess {

	@Invoker("getStructurePlacement")
	public abstract List<StructurePlacement> bigglobe_getStructurePlacement(RegistryEntry<Structure> entry, NoiseConfig noiseConfig);
}