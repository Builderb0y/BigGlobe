package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.structures.placement.StreamableStructurePlacement;

@Mixin(ConcentricRingsStructurePlacement.class)
public abstract class ConcentricRingsStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently extends StructurePlacement implements StreamableStructurePlacement {

	public ConcentricRingsStructurePlacement_ImplementStreamableStructurePlacementMoreEfficiently() {
		super(null, null, 0.0F, 0, null);
	}

	@Override
	public Stream<ChunkPos> bigglobe_getNearbyStartChunks(
		BigGlobeScriptedChunkGenerator generator,
		StructurePlacementCalculator calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		List<ChunkPos> list = calculator.getPlacementPositions((ConcentricRingsStructurePlacement)(Object)(this));
		if (list == null) return Stream.empty();
		return list.stream();
	}
}