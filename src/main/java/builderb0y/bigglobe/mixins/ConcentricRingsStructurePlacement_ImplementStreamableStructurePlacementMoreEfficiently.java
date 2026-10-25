package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
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
		ChunkGeneratorStructureState calculator,
		int centerChunkX,
		int centerChunkZ,
		int chunkRange
	) {
		List<ChunkPos> list = calculator.getRingPositionsFor((ConcentricRingsStructurePlacement)(Object)(this));
		if (list == null) return Stream.empty();
		return list.stream();
	}
}