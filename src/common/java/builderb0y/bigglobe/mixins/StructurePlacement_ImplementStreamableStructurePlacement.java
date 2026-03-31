package builderb0y.bigglobe.mixins;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

@Mixin(StructurePlacement.class)
public abstract class StructurePlacement_ImplementStreamableStructurePlacement implements StreamableStructurePlacement {

	@Shadow
	public abstract boolean isStructureChunk(ChunkGeneratorStructureState calculator, int chunkX, int chunkZ);

	@Override
	public Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		ChunkGeneratorStructureState calculator,
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
		.filter((ChunkPos chunkPos) -> this.isStructureChunk(calculator, chunkPos.x(), chunkPos.z()))
		;
	}
}