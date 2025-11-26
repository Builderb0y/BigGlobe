package builderb0y.bigglobe.structures;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.StructureAccessor;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.mixins.StructureAccessor_WorldAccess;
import builderb0y.bigglobe.overriders.Overrider.ColumnValueOverridersWithRadiusCache;
import builderb0y.bigglobe.scripting.wrappers.ArrayWrapper;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.StructureManager.StructureGenerationParams;

public class ScriptStructures extends ArrayWrapper<StructureStartWrapper> {

	public static final StructureStartWrapper[] EMPTY_STRUCTURE_START_ARRAY = {};
	public static final ScriptStructures EMPTY_SCRIPT_STRUCTURES = new ScriptStructures(EMPTY_STRUCTURE_START_ARRAY);

	public ScriptStructures(StructureStartWrapper[] starts) {
		super(starts);
	}

	public static ScriptStructures[] getStructures(
		BigGlobeScriptedChunkGenerator generator,
		ScriptedColumnLookup columns,
		StructureAccessor accessor,
		ChunkPos chunkPos,
		ColumnValueOverridersWithRadiusCache overriders
	) {
		StructureAccessor_WorldAccess structureAccessorAccessor = (StructureAccessor_WorldAccess)(accessor);
		if (structureAccessorAccessor.bigglobe_getWorld() instanceof ServerWorldAccess serverAccess) {
			return generator.structureManager.computeRelevantStructuresForOverriders(
				new StructureGenerationParams(
					generator,
					columns,
					serverAccess.toServerWorld(),
					chunkPos
				),
				overriders
			);
		}
		else {
			return new ScriptStructures[overriders.overriders().length];
		}
	}
}