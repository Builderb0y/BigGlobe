package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;

import builderb0y.bigglobe.mixinInterfaces.StructurePlacementCalculatorWithChunkGenerator;

@Mixin(StructurePlacementCalculator.class)
public class StructurePlacementCalculator_AddChunkGenerator implements StructurePlacementCalculatorWithChunkGenerator {

	@Unique
	private ChunkGenerator bigglobe_chunkGenerator;

	@Override
	public ChunkGenerator bigglobe_getChunkGenerator() {
		return this.bigglobe_chunkGenerator;
	}

	@Override
	public void bigglobe_setChunkGenerator(ChunkGenerator generator) {
		this.bigglobe_chunkGenerator = generator;
	}
}