package builderb0y.bigglobe.mixins;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

@Mixin(StructurePlacement.class)
public abstract class StructurePlacement_ImplementStreamableStructurePlacement implements StreamableStructurePlacement {

	@Shadow public abstract boolean shouldGenerate(StructurePlacementCalculator calculator, int chunkX, int chunkZ);

	@Override
	public Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		StructurePlacementCalculator calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		return IntStream.rangeClosed(centerChunkZ - chunkRange, centerChunkZ + chunkRange).mapToObj((int chunkZ) -> {
			return IntStream.rangeClosed(centerChunkX - chunkRange, centerChunkX + chunkRange).mapToObj((int chunkX) -> {
				return new ChunkPos(chunkX, chunkZ);
			});
		})
		.flatMap(Function.identity())
		.filter((ChunkPos chunkPos) -> this.shouldGenerate(calculator, chunkPos.x, chunkPos.z))
		;
	}
}