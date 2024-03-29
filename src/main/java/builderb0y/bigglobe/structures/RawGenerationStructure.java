package builderb0y.bigglobe.structures;

import net.minecraft.structure.StructurePiece;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;

/**
a {@link Structure} which has at least one StructurePiece which
implements {@link RawGenerationStructurePiece}.
*/
public interface RawGenerationStructure {

	/**
	a {@link StructurePiece} which can place blocks during raw chunk generation,
	before features and other structures are placed.
	this is handled by my chunk generators,
	and therefore will not work outside of big globe worlds.
	*/
	public static interface RawGenerationStructurePiece {

		public abstract void generateRaw(Context context);

		public static class Context {

			public long worldSeed, pieceSeed;
			public Chunk chunk;
			public BigGlobeChunkGenerator generator;
			public ChunkOfColumns<? extends WorldColumn> columns;
			public boolean distantHorizons;

			public Context(
				long pieceSeed,
				Chunk chunk,
				BigGlobeChunkGenerator generator,
				ChunkOfColumns<? extends WorldColumn> columns,
				boolean distantHorizons
			) {
				this.worldSeed       = generator.seed;
				this.pieceSeed       = pieceSeed;
				this.chunk           = chunk;
				this.generator       = generator;
				this.columns         = columns;
				this.distantHorizons = distantHorizons;
			}
		}
	}
}