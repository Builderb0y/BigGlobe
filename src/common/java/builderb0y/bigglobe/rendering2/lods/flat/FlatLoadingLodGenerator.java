package builderb0y.bigglobe.rendering2.lods.flat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.rendering2.lods.LodGenerator;

public class FlatLoadingLodGenerator extends LodGenerator<FlatLoadingLodGenerator.LightweightChunkArea> {

	public final ChunkCache chunkCache;

	@Override
	public void close() {
		super.close();
		this.chunkCache.close();
	}

	public FlatLoadingLodGenerator(ClientGeneratorParams generatorParams, ServerLevel world) {
		super(generatorParams, world.dimensionType());
		this.chunkCache = new ChunkCache(this, world);
	}

	@Override
	public LightweightChunkArea preload(BoundingBox area) {
		return new LightweightChunkArea(
			this.chunkCache.getChunks(
				new ChunkPos(area.minX() >> 4, area.minZ() >> 4),
				new ChunkPos(area.maxX() >> 4, area.maxZ() >> 4)
			)
		);
	}

	@Override
	public QuadList loadOrGenerate(QuadColumn quadColumn, LoadMode mode, LightweightChunkArea chunks) {
		if (chunks != null) {
			QuadList list = new QuadList();
			list.object00 = this.fetch(chunks, quadColumn.object00);
			list.object01 = this.fetch(chunks, quadColumn.object01);
			list.object10 = this.fetch(chunks, quadColumn.object10);
			list.object11 = this.fetch(chunks, quadColumn.object11);
			if (list.anyNull()) {
				QuadList generated = super.loadOrGenerate(quadColumn, mode, chunks);
				list.fillNullsFrom(generated);
			}
			return list;
		}
		else {
			return super.loadOrGenerate(quadColumn, mode, chunks);
		}
	}

	public BlockSegmentList fetch(LightweightChunkArea chunks, ScriptedColumn column) {
		LightweightChunk chunk = chunks.getChunk(column.x(), column.z());
		if (chunk != null) {
			return chunk.getColumn(column.x(), column.z(), column.hints().lod());
		}
		return null;
	}

	public static record LightweightChunkArea(LightweightChunk[] chunks, int stride) {

		public LightweightChunkArea(LightweightChunk[] chunks) {
			this(chunks, chunks[chunks.length - 1].pos.x() - chunks[0].pos.x() + 1);
		}

		public LightweightChunk getChunk(int blockX, int blockZ) {
			ChunkPos firstPos = this.chunks[0].pos;
			int chunkX = (blockX >> 4) - firstPos.x();
			int chunkZ = (blockZ >> 4) - firstPos.z();
			return this.chunks[chunkZ * this.stride + chunkX];
		}
	}
}