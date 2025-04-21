package builderb0y.bigglobe.structures;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.mixins.StructureAccessor_WorldAccess;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.StructureManager.FinalStructures;
import builderb0y.bigglobe.structures.StructureManager.StructureGenerationParams;
import builderb0y.bigglobe.versions.RegistryVersions;

public class ScriptStructures extends ArrayWrapper<StructureStartWrapper> {

	public static final StructureStartWrapper[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final ScriptStructures EMPTY_SCRIPT_STRUCTURES = new ScriptStructures(EMPTY_STRUCTURE_START_ARRAY);

	public ScriptStructures(StructureStartWrapper[] starts) {
		super(starts);
	}

	public static ScriptStructures getStructures(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
		StructureAccessor accessor,
		ChunkPos chunkPos
	) {
		StructureAccessor_WorldAccess structureAccessorAccessor = (StructureAccessor_WorldAccess)(accessor);
		if (structureAccessorAccessor.bigglobe_getWorld() instanceof ServerWorldAccess serverAccess) {
			FinalStructures starts = generator.structureManager.getIntersectingStructures(
				new StructureGenerationParams(
					generator,
					columns,
					serverAccess.toServerWorld(),
					chunkPos
				)
			);
			if (starts.isEmpty()) {
				return EMPTY_SCRIPT_STRUCTURES;
			}
			//note: need an actual Registry instance here due to its
			//ability to lookup the key associated with a given object.
			Registry<Structure> structureRegistry = RegistryVersions.getRegistry(
				BigGlobeMod.getCurrentServer().getRegistryManager(),
				RegistryKeys.STRUCTURE
			);
			return new ScriptStructures(
				starts
					.stream()
					.map((StructureStart start) -> StructureStartWrapper.of(
						RegistryVersions.getEntry(
							structureRegistry,
							start.getStructure()
						),
						start
					))
					.toArray(StructureStartWrapper[]::new)
			);
		}
		else {
			return EMPTY_SCRIPT_STRUCTURES;
		}
	}
}