package builderb0y.bigglobe.rendering2.lods.flat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.rendering2.lods.LodGenerator;
import builderb0y.bigglobe.rendering2.lods.LodSystem;

@Environment(EnvType.CLIENT)
public class FlatLoadingLodGenerator extends LodGenerator<FlatLoadingLodGenerator.LightweightChunkArea> {

	public final ChunkCache chunkCache;

	@Override
	public String f3Message() {
		return super.f3Message() + ", " + this.chunkCache.f3Message();
	}

	@Override
	public void close() {
		super.close();
		this.chunkCache.close();
	}

	public FlatLoadingLodGenerator(LodSystem system, ServerLevel world) {
		super(system, world.dimensionType());
		this.chunkCache = new ChunkCache(this, world);
	}

	@Override
	public LightweightChunkArea preload(BoundingBox area) {
		ChunkPos minPos = new ChunkPos(area.minX() >> 4, area.minZ() >> 4);
		ChunkPos maxPos = new ChunkPos(area.maxX() >> 4, area.maxZ() >> 4);
		return new LightweightChunkArea(
			this.chunkCache.getChunks(
				minPos,
				maxPos
			),
			minPos,
			maxPos
		);
	}

	@Override
	public QuadList loadOrGenerate(QuadColumn quadColumn, LoadMode mode, DownscaleSettings downscale, LightweightChunkArea chunks) {
		if (chunks != null) {
			QuadList list = new QuadList();
			list.object00 = this.fetch(chunks, quadColumn.object00);
			list.object01 = this.fetch(chunks, quadColumn.object01);
			list.object10 = this.fetch(chunks, quadColumn.object10);
			list.object11 = this.fetch(chunks, quadColumn.object11);
			if (list.anyNull()) {
				list.fillNullsFrom(super.loadOrGenerate(quadColumn, mode, downscale, chunks));
			}
			return list;
		}
		else {
			return super.loadOrGenerate(quadColumn, mode, downscale, chunks);
		}
	}

	public BlockSegmentList fetch(LightweightChunkArea chunks, ScriptedColumn column) {
		LightweightChunk chunk = chunks.getChunk(column.x(), column.z());
		if (chunk != null) {
			byte lod = column.hints().lod();
			return chunk.getColumn(column.x() >> lod, column.z() >> lod, lod);
		}
		return null;
	}

	@Override
	public void processDirtyChunks() {
		this.chunkCache.processDirtyChunks();
	}

	@Environment(EnvType.CLIENT)
	public static record LightweightChunkArea(LightweightChunk[] chunks, int minChunkX, int minChunkZ, int stride) {

		public LightweightChunkArea(LightweightChunk[] chunks, ChunkPos minChunkPosInclusive, ChunkPos maxChunkPosInclusive) {
			this(chunks, minChunkPosInclusive.x(), minChunkPosInclusive.z(), maxChunkPosInclusive.x() - minChunkPosInclusive.x() + 1);
		}

		public LightweightChunk getChunk(int blockX, int blockZ) {
			int chunkX = (blockX >> 4) - this.minChunkX;
			int chunkZ = (blockZ >> 4) - this.minChunkZ;
			return this.chunks[chunkZ * this.stride + chunkX];
		}
	}
}