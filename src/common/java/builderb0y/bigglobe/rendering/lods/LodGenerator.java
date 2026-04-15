package builderb0y.bigglobe.rendering.lods;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.chunkgen.QuadHolder;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.lods.flat.LightweightChunk.ColumnIndexRange;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.SafeCloseable;

@Environment(EnvType.CLIENT)
public class LodGenerator<T_LoadCache> implements SafeCloseable {

	public final LodSystem system;
	public final DimensionType dimensionType;
	public final ConcurrentLinkedQueue<ScriptedColumn> columnRecycler = new ConcurrentLinkedQueue<>();
	public final LongAdder storedColumnCount = new LongAdder();
	public final byte maxLoadLevel;

	public LodGenerator(LodSystem system, DimensionType dimensionType) {
		this.system = system;
		this.dimensionType = dimensionType;
		this.maxLoadLevel = (byte)(BigGlobeConfig.INSTANCE.get().lodRendering.maxLodForChunkLoading);
	}

	public String f3Message() {
		return "[BG] LOD Generator: columns: " + this.storedColumnCount.longValue();
	}

	public ScriptedColumn nextRecycledColumn(ScriptedColumn.Params params) {
		ScriptedColumn column = this.columnRecycler.poll();
		if (column != null) {
			this.storedColumnCount.decrement();
			column.setParamsUnchecked(params);
			return column;
		}
		else {
			return this.system.params.columnEntryRegistry.columnFactory.create(params);
		}
	}

	public void recycleColumn(ScriptedColumn column) {
		this.columnRecycler.add(column);
		this.storedColumnCount.increment();
	}

	public void recycleColumns(ScriptedColumn... columns) {
		this.columnRecycler.addAll(Arrays.asList(columns));
		this.storedColumnCount.add(columns.length);
	}

	public void recycleColumns(Collection<ScriptedColumn> columns) {
		this.columnRecycler.addAll(columns);
		this.storedColumnCount.add(columns.size());
	}

	public @Nullable ColumnBlockGetter generateRegion(
		BoundingBox generateFrom,
		BoundingBox translateTo,
		byte lod,
		LoadMode mode,
		DownscaleSettings downscale,
		T_LoadCache cache
	) {
		int distanceBetweenColumns = 1 << lod;
		int distanceBetweenQuads = 2 << lod;
		int shift = downscale.mergeHorizontally() ? lod + 1 : lod;
		int columnCount = (generateFrom.getXSpan() >> shift) * (generateFrom.getZSpan() >> shift);
		ScriptedColumn[] columns = new ScriptedColumn[columnCount];
		BlockSegmentList[] lists = new BlockSegmentList[columnCount];
		ScriptedColumn.Params params = new Params(
			this.system.params.columnSeed,
			0,
			0,
			generateFrom.minY(),
			generateFrom.maxY() + 1,
			ColumnUsage.RAW_GENERATION.builtinLodHints(lod),
			this.system.params.compiledWorldTraits
		);
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int z = generateFrom.minZ(); z <= generateFrom.maxZ(); z += distanceBetweenQuads) {
				final int z_ = z;
				for (int x = generateFrom.minX(); x <= generateFrom.maxX(); x += distanceBetweenQuads) {
					final int x_ = x;
					async.submit(() -> {
						QuadColumn quadColumn = new QuadColumn(
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params)
						);
						quadColumn.at(params, x_, z_, distanceBetweenColumns);
						QuadList quadList = this.loadOrGenerate(quadColumn, mode, downscale, cache);
						int relativeX = (x_ - generateFrom.minX())    >> shift;
						int relativeZ = (z_ - generateFrom.minZ())    >> shift;
						int stride    =       generateFrom.getXSpan() >> shift;
						int baseIndex = relativeZ * stride + relativeX;
						if (downscale.mergeHorizontally()) {
							BlockSegmentList merged = quadList.merge();
							lists[baseIndex] = merged;
							columns[baseIndex] = quadColumn.object00;
							this.recycleColumns(quadColumn.object01, quadColumn.object10, quadColumn.object11);
						}
						else {
							quadColumn.storeInArray(columns, baseIndex, stride);
							quadList.storeInArray(lists, baseIndex, stride);
						}
					});
				}
			}
		}
		boolean redo;
		if (mode.canFail()) {
			boolean haveNull = false, haveNonNull = false;
			for (BlockSegmentList list : lists) {
				if (list == null) haveNull = true;
				else haveNonNull = true;
			}
			if (haveNull) {
				if (haveNonNull) {
					redo = true;
				}
				else {
					this.recycleColumns(columns);
					return null;
				}
			}
			else {
				redo = false;
			}
		}
		else {
			redo = false;
		}
		if (redo) try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int z = generateFrom.minZ(); z <= generateFrom.maxZ(); z += distanceBetweenQuads) {
				final int z_ = z;
				for (int x = generateFrom.minX(); x <= generateFrom.maxX(); x += distanceBetweenQuads) {
					final int x_ = x;
					async.submit(() -> {
						int relativeX = (x_ - generateFrom.minX())    >> lod;
						int relativeZ = (z_ - generateFrom.minZ())    >> lod;
						int stride    =       generateFrom.getXSpan() >> lod;
						int baseIndex = relativeZ * stride + relativeX;
						if (downscale.mergeHorizontally()) {
							if (lists[baseIndex] == null) {
								QuadColumn quadColumn = new QuadColumn(
									this.nextRecycledColumn(params),
									this.nextRecycledColumn(params),
									this.nextRecycledColumn(params),
									this.nextRecycledColumn(params)
								);
								quadColumn.at(params, x_, z_, distanceBetweenColumns);
								QuadList quadList = this.loadOrGenerate(quadColumn, LoadMode.GENERATE_ONLY, downscale, cache);
								lists[baseIndex] = quadList.merge();
								this.recycleColumns(quadColumn.object01, quadColumn.object10, quadColumn.object11);
							}
						}
						else {
							QuadList loaded = new QuadList();
							loaded.loadFromArray(lists, baseIndex, stride);
							if (loaded.anyNull()) {
								QuadColumn quadColumn = new QuadColumn();
								quadColumn.loadFromArray(columns, baseIndex, stride);
								QuadList generated = this.loadOrGenerate(quadColumn, LoadMode.GENERATE_ONLY, downscale, cache);
								loaded.fillNullsFrom(generated);
								loaded.storeInArray(lists, baseIndex, stride);
							}
						}
					});
				}
			}
		}
		return new ColumnBlockGetter(
			lists,
			columns,
			translateTo,
			translateTo, //not padded here.
			lod,
			this.dimensionType.hasSkyLight() ? (byte)(15) : (byte)(0),
			this.system.params.colors,
			this.system.params.biomeSource,
			this.dimensionType.cardinalLightType().get(),
			this::recycleColumns
		);
	}

	public T_LoadCache preload(BoundingBox area) {
		return null;
	}

	public QuadList loadOrGenerate(QuadColumn quadColumn, LoadMode mode, DownscaleSettings downscale, @Nullable T_LoadCache cache) {
		QuadList quadList = new QuadList();
		if (mode.canGenerate()) {
			quadList.createNew(quadColumn.object00.minY(), quadColumn.object00.maxY());
			QuadHolder.generate(quadColumn, quadList, this.system.params.layer.value());
			downscale.applyDownscale(quadList);
			quadList.computeLightLevels(this.dimensionType.hasSkyLight() ? ((byte)(15)) : ((byte)(0)));
		}
		return quadList;
	}

	public void processDirtyChunks() {}

	@Environment(EnvType.CLIENT)
	public static enum LoadMode {
		GENERATE_ONLY,
		LOAD_OR_GENERATE,
		LOAD_ONLY;

		public boolean canLoad() {
			return this != GENERATE_ONLY;
		}

		public boolean canGenerate() {
			return this != LOAD_ONLY;
		}

		public boolean canFail() {
			return this == LOAD_ONLY;
		}

		public LoadMode allowGeneration() {
			return this == LOAD_ONLY ? LOAD_OR_GENERATE : this;
		}
	}

	@Environment(EnvType.CLIENT)
	public static record DownscaleSettings(byte packedData) {

		public static final DownscaleSettings NONE = new DownscaleSettings((byte)(0));

		public DownscaleSettings(int packedData) {
			this((byte)(packedData));
		}

		public boolean mergeHorizontally() {
			return (this.packedData & 128) != 0;
		}

		public DownscaleSettings mergeHorizontally(boolean mergeHorizontally) {
			return new DownscaleSettings(mergeHorizontally ? (this.packedData | 128) : (this.packedData & ~128));
		}

		public int deltaLod() {
			return this.packedData & 31;
		}

		public DownscaleSettings deltaLod(int deltaLod) {
			return new DownscaleSettings((this.packedData & ~31) | deltaLod);
		}

		public boolean keepAir() {
			return (this.packedData & 64) != 0;
		}

		public DownscaleSettings keepAir(boolean keepAir) {
			return new DownscaleSettings(keepAir ? (this.packedData | 64) : (this.packedData & ~64));
		}

		public void applyDownscale(QuadList list) {
			if (this.deltaLod() > 0) {
				if (this.keepAir()) {
					list.downscaleKeepAir(this.deltaLod());
				}
				else {
					list.downscale(this.deltaLod());
				}
			}
		}
	}

	public BlockSegmentList[] generateCaveCullingChunk(ChunkPos chunkPos) {
		BlockSegmentList[] lists = new BlockSegmentList[ColumnIndexRange.LOD4.end];
		ScriptedColumn.Params params = new Params(
			this.system.params.columnSeed,
			0,
			0,
			this.system.params.minY,
			this.system.params.maxY,
			ColumnUsage.HEIGHTMAP.builtinLodHints(0),
			this.system.params.compiledWorldTraits
		);
		Layer layer = this.system.params.layer.value();
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			int
				minX = chunkPos.x() << 4,
				minZ = chunkPos.z() << 4,
				maxX = minX + 16,
				maxZ = minZ + 16;
			for (int z = minZ; z < maxZ; z += 2) {
				final int z_ = z;
				for (int x = minX; x < maxX; x += 2) {
					final int x_ = x;
					async.submit(() -> {
						QuadColumn quadColumn = new QuadColumn(
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params)
						);
						quadColumn.at(params, x_, z_, 1);
						QuadList quadList = new QuadList();
						quadList.createNew(params.minY(), params.maxY());
						QuadHolder.generate(quadColumn, quadList, layer);
						this.recycleColumns(quadColumn.object00, quadColumn.object01, quadColumn.object10, quadColumn.object11);
						int relativeX = (x_ - minX);
						int relativeZ = (z_ - minZ);
						int baseIndex = (relativeZ << 4) + relativeX;
						quadList.storeInArray(lists, baseIndex, 16);
					});
				}
			}
		}
		for (int srcLod = 0; srcLod < 4; srcLod++) {
			int dstLod = srcLod + 1;
			int srcShift = 4 - srcLod;
			int dstShift = 4 - dstLod;
			int dstChunkSize = 1 << dstShift;
			int srcBase = ColumnIndexRange.VALUES[srcLod].start;
			int dstBase = ColumnIndexRange.VALUES[dstLod].start;
			for (int srcZ = 0, dstZ = 0; dstZ < dstChunkSize; srcZ += 2, dstZ += 1) {
				for (int srcX = 0, dstX = 0; dstX < dstChunkSize; srcX += 2, dstX += 1) {
					int srcIndex = ((srcZ << srcShift) | srcX) + srcBase;
					int dstIndex = ((dstZ << dstShift) | dstX) + dstBase;
					lists[dstIndex] = QuadList.downscaleColumn(lists[srcIndex], 1);
				}
			}
		}
		return lists;
	}

	@Override
	public void close() {

	}
}