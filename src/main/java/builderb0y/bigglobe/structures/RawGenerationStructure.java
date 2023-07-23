package builderb0y.bigglobe.structures;

import net.minecraft.structure.StructurePiece;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;

/**
a {@link Structure} which has at least one StructurePiece which
implements {@link RawGenerationStructurePiece}.
*/
public interface RawGenerationStructure {

	public static void generateAll(ScriptStructures structures, long seed, Chunk chunk, ChunkOfColumns<? extends WorldColumn> columns, boolean distantHorizons) {
		RawGenerationStructurePiece.Context rawGenerationContext = null;
		for (StructureStartWrapper start : structures.elements) {
			if (start.structure().entry.value() instanceof RawGenerationStructure) {
				for (StructurePiece piece : start.pieces()) {
					if (piece instanceof RawGenerationStructurePiece raw) {
						if (rawGenerationContext == null) {
							rawGenerationContext = new RawGenerationStructurePiece.Context(seed, chunk, structures, columns, distantHorizons);
						}
						raw.generateRaw(rawGenerationContext);
					}
				}
			}
		}
	}

	/**
	a {@link StructurePiece} which can place blocks during raw chunk generation,
	before features and other structures are placed.
	this is handled by my chunk generators,
	and therefore will not work outside of big globe worlds.
	*/
	public static interface RawGenerationStructurePiece {

		public abstract void generateRaw(Context context);

		public static class Context {

			public long seed;
			public Chunk chunk;
			public ScriptStructures structures;
			public ChunkOfColumns<? extends WorldColumn> columns;
			public boolean distantHorizons;

			public Context(
				long seed,
				Chunk chunk,
				ScriptStructures structures,
				ChunkOfColumns<? extends WorldColumn> columns,
				boolean distantHorizons
			) {
				this.seed            = seed;
				this.chunk           = chunk;
				this.structures      = structures;
				this.columns         = columns;
				this.distantHorizons = distantHorizons;
			}
		}
	}
}