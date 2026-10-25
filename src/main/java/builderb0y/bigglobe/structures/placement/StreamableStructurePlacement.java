package builderb0y.bigglobe.structures.placement;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.mixins.ConcentricRingsStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently;

/**
implemented by mixin on {@link StructurePlacement} and its vanilla subclasses.
*/
public interface StreamableStructurePlacement {

	/**
	returns a Stream containing all the closest ChunkPos's that
	the associated structure(s) can potentially start in.
	the returned Stream should, in general, contain
	chunk positions which meet the following criteria:
	{@code
	Math.max(
	Math.abs(chunkPos.x - centerChunkX),
	Math.abs(chunkPos.z - centerChunkZ)
	)
	<= chunkRange
	}
	however, this is not a strong requirement.
	{@link ConcentricRingsStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently}
	violates this criteria and instead returns all valid spawn positions,
	because this logic is used by eyes of ender to find strongholds.
	it is more desirable to return an out-of-range stronghold
	than no stronghold at all.
	*/
	public abstract Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		ChunkGeneratorStructureState calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	);

	public default Stream<ChunkPos> bigglobe_getFilteredStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		ChunkGeneratorStructureState calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		return this.bigglobe_getNearbyStartChunks(
				generator,
				calculator,
				centerChunkX,
				centerChunkZ,
				chunkRange
			)
				.filter(rangeFilter(centerChunkX, centerChunkZ, chunkRange));
	}

	public static int distance(ChunkPos chunkPos, int centerChunkX, int centerChunkZ) {
		return Math.max(
			Math.abs(chunkPos.x - centerChunkX),
			Math.abs(chunkPos.z - centerChunkZ)
		);
	}

	public static Predicate<ChunkPos> rangeFilter(int centerChunkX, int centerChunkZ, int chunkRange) {
		return (ChunkPos chunkPos) -> distance(chunkPos, centerChunkX, centerChunkZ) <= chunkRange;
	}

	public static Comparator<ChunkPos> distanceComparator(int centerChunkX, int centerChunkZ) {
		return Comparator.comparingInt((ChunkPos chunkPos) -> distance(chunkPos, centerChunkX, centerChunkZ));
	}
}