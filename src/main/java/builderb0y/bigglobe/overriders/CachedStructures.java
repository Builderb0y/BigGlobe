package builderb0y.bigglobe.overriders;

import java.util.List;

import com.google.common.base.Predicates;
import org.jetbrains.annotations.Nullable;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.StructureAccessor;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.structures.LakeStructure;

public class CachedStructures {

	public static final StructureStart[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final CachedStructures EMPTY = new CachedStructures(EMPTY_STRUCTURE_START_ARRAY);

	public final StructureStart[] starts;
	public final LakeStructure.@Nullable Piece lake;

	public CachedStructures(StructureStart[] starts) {
		this.starts = starts;
		this.lake = findLake(starts);
	}

	public static CachedStructures getStructures(StructureAccessor structureAccessor, ChunkPos chunkPos, boolean distantHorizons) {
		if (distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures) {
			return EMPTY;
		}
		else {
			List<StructureStart> starts = structureAccessor.getStructureStarts(chunkPos, Predicates.alwaysTrue());
			return starts.isEmpty() ? EMPTY : new CachedStructures(starts.toArray(new StructureStart[starts.size()]));
		}
	}

	public static LakeStructure.@Nullable Piece findLake(StructureStart[] starts) {
		for (StructureStart start : starts) {
			if (start.getStructure() instanceof LakeStructure) {
				return ((LakeStructure.Piece)(start.getChildren().get(0)));
			}
		}
		return null;
	}
}