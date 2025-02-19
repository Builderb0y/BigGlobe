package builderb0y.bigglobe.mixins;

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently extends StructurePlacement implements StreamableStructurePlacement {

	@Shadow @Final private int spacing;

	public RandomSpreadStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently() {
		super(null, null, 0.0F, 0, null);
	}

	@Shadow public abstract ChunkPos getStartChunk(long seed, int chunkX, int chunkZ);

	@Override
	public Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		StructurePlacementCalculator calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		int spacing = this.spacing;
		long seed = calculator.getStructureSeed();
		int regionMinX = Math.floorDiv(centerChunkX - chunkRange, spacing);
		int regionMaxX = Math.floorDiv(centerChunkX + chunkRange, spacing);
		int regionMinZ = Math.floorDiv(centerChunkZ - chunkRange, spacing);
		int regionMaxZ = Math.floorDiv(centerChunkZ + chunkRange, spacing);
		return IntStream.rangeClosed(regionMinZ, regionMaxZ).map((int regionZ) -> regionZ * spacing).mapToObj((int chunkZ) -> {
			return IntStream.rangeClosed(regionMinX, regionMaxX).map((int regionX) -> regionX * spacing).mapToObj((int chunkX) -> {
				return this.getStartChunk(seed, chunkX, chunkZ);
			});
		})
		.flatMap(Function.identity())
		.filter((ChunkPos chunkPos) -> this.shouldGenerate(calculator, chunkPos.x, chunkPos.z))
		;
	}
}