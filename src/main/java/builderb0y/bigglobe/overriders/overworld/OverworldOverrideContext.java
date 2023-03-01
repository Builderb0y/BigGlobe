package builderb0y.bigglobe.overriders.overworld;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.CachedStructures;

public class OverworldOverrideContext {

	public final BigGlobeOverworldChunkGenerator generator;
	public final OverworldColumn column;
	public final CachedStructures structures;
	public final OverridePhase phase;

	public OverworldOverrideContext(
		BigGlobeOverworldChunkGenerator generator,
		OverworldColumn column,
		CachedStructures structures,
		OverridePhase phase
	) {
		this.column = column;
		this.generator = generator;
		this.structures = structures;
		this.phase = phase;
	}

	public static enum OverridePhase {
		RAW_TERRAIN,
		DECORATION;
	}
}