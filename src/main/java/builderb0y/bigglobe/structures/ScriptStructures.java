package builderb0y.bigglobe.structures;

import java.util.List;

import com.google.common.base.Predicates;

import net.minecraft.registry.Registry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

public class ScriptStructures extends ArrayWrapper<StructureStartWrapper> {

	public static final StructureStartWrapper[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final ScriptStructures EMPTY_SCRIPT_STRUCTURES = new ScriptStructures(EMPTY_STRUCTURE_START_ARRAY);

	public ScriptStructures(StructureStartWrapper[] starts) {
		super(starts);
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
}