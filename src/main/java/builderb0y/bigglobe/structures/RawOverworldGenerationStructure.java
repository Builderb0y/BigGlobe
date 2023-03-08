package builderb0y.bigglobe.structures;

import net.minecraft.structure.StructurePiece;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;

/**
a {@link Structure} which has at least one StructurePiece which
implements {@link RawOverworldGenerationStructurePiece}.
*/
public interface RawOverworldGenerationStructure {

	/**
	a {@link StructurePiece} which can place blocks during raw chunk generation,
	before features and other structures are placed.
	right now, {@link BigGlobeOverworldChunkGenerator} handles this,
	so this mechanism only works in the overworld.
	a future expansion may extend this logic to the nether and end.
	*/
	public static interface RawOverworldGenerationStructurePiece {

		public abstract void generateRaw(Context context);

		public static class Context {

			public long seed;
			public Chunk chunk;
			public ScriptStructures structures;
			public ChunkOfColumns<OverworldColumn> columns;
			public boolean distantHorizons;

			public Context(
				long seed,
				Chunk chunk,
				ScriptStructures structures,
				ChunkOfColumns<OverworldColumn> columns,
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