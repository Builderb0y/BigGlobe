package builderb0y.bigglobe.mixins;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.management.SmartStructurePlacement;

@Mixin(StructurePlacement.class)
public abstract class StructurePlacement_MakeSmart implements SmartStructurePlacement {

	@Shadow
	public abstract boolean isStructureChunk(ChunkGeneratorStructureState calculator, int chunkX, int chunkZ);

	@Override
	public Stream<StructureStartWrapper> bigglobe_generateStructuresInArea(Context context) {
		int
			minChunkX = context.area().minX() >> 4,
			minChunkZ = context.area().minZ() >> 4,
			maxChunkX = context.area().maxX() >> 4,
			maxChunkZ = context.area().maxZ() >> 4;
		return IntStream.rangeClosed(minChunkZ, maxChunkZ).mapToObj((int chunkZ) -> {
			return IntStream.rangeClosed(minChunkX, maxChunkX).mapToObj((int chunkX) -> {
				return new ChunkPos(chunkX, chunkZ);
			});
		})
		.flatMap(Function.identity())
		.filter((ChunkPos chunkPos) -> this.isStructureChunk(context.structureState(), chunkPos.x(), chunkPos.z()))
		.map(context::createRandomFromSetAt)
		.filter(Objects::nonNull);
	}
}