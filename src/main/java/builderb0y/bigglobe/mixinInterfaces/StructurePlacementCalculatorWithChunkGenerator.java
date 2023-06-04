package builderb0y.bigglobe.mixinInterfaces;

import net.minecraft.world.gen.chunk.ChunkGenerator;

public interface StructurePlacementCalculatorWithChunkGenerator {

	public abstract ChunkGenerator bigglobe_getChunkGenerator();

	public abstract void bigglobe_setChunkGenerator(ChunkGenerator generator);
}