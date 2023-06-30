package builderb0y.bigglobe.overriders;

import java.util.AbstractList;
import java.util.List;

import com.google.common.base.Predicates;

import net.minecraft.registry.Registry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.LakeStructure;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class ScriptStructures extends AbstractList<StructureStartWrapper> {

	public static final StructureStartWrapper[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final ScriptStructures EMPTY_SCRIPT_STRUCTURES = new ScriptStructures(EMPTY_STRUCTURE_START_ARRAY);

	public final StructureStartWrapper[] starts;
	public final LakeStructure.Piece lake; //used frequently in other hard-coded areas, so might as well cache it.

	public ScriptStructures(StructureStartWrapper[] starts) {
		this.starts = starts;
		this.lake = this.findLake();
	}

	public static ScriptStructures getStructures(StructureAccessor structureAccessor, ChunkPos chunkPos, boolean distantHorizons) {
		if (distantHorizons && BigGlobeConfig.INSTANCE.get().distantHorizonsIntegration.skipStructures) {
			return EMPTY_SCRIPT_STRUCTURES;
		}
		List<StructureStart> starts = structureAccessor.getStructureStarts(chunkPos, Predicates.alwaysTrue());
		if (starts.isEmpty()) {
			return EMPTY_SCRIPT_STRUCTURES;
		}
		Registry<Structure> structureRegistry = BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeyVersions.structure());
		return new ScriptStructures(
			starts
			.stream()
			.map(start -> StructureStartWrapper.of(
				structureRegistry.entryOf(
					UnregisteredObjectException.getKey(
						structureRegistry,
						start.getStructure()
					)
				),
				start
			))
			.toArray(StructureStartWrapper[]::new)
		);
	}

	@Override
	public StructureStartWrapper get(int index) {
		return this.starts[index];
	}

	@Override
	public int size() {
		return this.starts.length;
	}

	public LakeStructure.Piece findLake() {
		for (StructureStartWrapper start : this.starts) {
			if (start.structure().entry.value() instanceof LakeStructure) {
				return ((LakeStructure.Piece)(start.pieces().get(0)));
			}
		}
		return null;
	}
}