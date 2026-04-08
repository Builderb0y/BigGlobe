package builderb0y.bigglobe.rendering2.lods;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.chunkgen.QuadHolder;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.SafeCloseable;

public class LodGenerator<T> implements SafeCloseable {

	public final ClientGeneratorParams generatorParams;
	public final DimensionType dimensionType;
	public final ConcurrentLinkedQueue<ScriptedColumn> columnRecycler = new ConcurrentLinkedQueue<>();
	public final AtomicInteger storedColumnCount = new AtomicInteger();
	public final byte maxLoadLevel;

	public LodGenerator(ClientGeneratorParams generatorParams, DimensionType dimensionType) {
		this.generatorParams = generatorParams;
		this.dimensionType = dimensionType;
		this.maxLoadLevel = (byte)(BigGlobeConfig.INSTANCE.get().lodRendering.maxLodForChunkLoading);
	}

	public ScriptedColumn nextRecycledColumn(ScriptedColumn.Params params) {
		ScriptedColumn column = this.columnRecycler.poll();
		if (column != null) {
			this.storedColumnCount.decrementAndGet();
			column.setParamsUnchecked(params);
			return column;
		}
		else {
			return this.generatorParams.columnEntryRegistry.columnFactory.create(params);
		}
	}

	public @Nullable ColumnBlockGetter generateRegion(BoundingBox region, byte lod, LoadMode mode) {
		int distanceBetweenColumns = 1 << lod;
		int distanceBetweenQuads = 2 << lod;
		int columnCount = (region.getXSpan() >> lod) * (region.getZSpan() >> lod);
		ScriptedColumn[] columns = new ScriptedColumn[columnCount];
		BlockSegmentList[] lists = new BlockSegmentList[columnCount];
		ScriptedColumn.Params params = new Params(
			this.generatorParams.columnSeed,
			0,
			0,
			region.minY(),
			region.maxY() + 1,
			ColumnUsage.RAW_GENERATION.builtinLodHints(lod),
			this.generatorParams.compiledWorldTraits
		);
		T cache = mode.canLoad() && lod < this.maxLoadLevel ? this.preload(region) : null;
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int z = region.minZ(); z <= region.maxZ(); z += distanceBetweenQuads) {
				final int z_ = z;
				for (int x = region.minX(); x <= region.maxX(); x += distanceBetweenQuads) {
					final int x_ = x;
					async.submit(() -> {
						QuadColumn quadColumn = new QuadColumn(
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params),
							this.nextRecycledColumn(params)
						);
						quadColumn.at(params, x_, z_, distanceBetweenColumns);
						QuadList quadList = this.loadOrGenerate(quadColumn, mode, cache);
						int relativeX = (x_ - region.minX())    >> lod;
						int relativeZ = (z_ - region.minZ())    >> lod;
						int stride    =       region.getXSpan() >> lod;
						int baseIndex = relativeZ * stride + relativeX;
						quadColumn.storeInArray(columns, baseIndex, stride);
						quadList.storeInArray(lists, baseIndex, stride);
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
					this.columnRecycler.addAll(Arrays.asList(columns));
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
			for (int z = region.minZ(); z <= region.maxZ(); z += distanceBetweenQuads) {
				final int z_ = z;
				for (int x = region.minX(); x <= region.maxX(); x += distanceBetweenQuads) {
					final int x_ = x;
					async.submit(() -> {
						QuadList loaded = new QuadList();
						int relativeX = (x_ - region.minX())    >> lod;
						int relativeZ = (z_ - region.minZ())    >> lod;
						int stride    =       region.getXSpan() >> lod;
						int baseIndex = relativeZ * stride + relativeX;
						loaded.loadFromArray(lists, baseIndex, stride);
						if (loaded.anyNull()) {
							QuadColumn quadColumn = new QuadColumn();
							quadColumn.loadFromArray(columns, baseIndex, stride);
							QuadList generated = this.loadOrGenerate(quadColumn, LoadMode.GENERATE_ONLY, cache);
							loaded.fillNullsFrom(generated);
							loaded.storeInArray(lists, baseIndex, stride);
						}
					});
				}
			}
		}
		BoundingBox transformedVolume = new BoundingBox(
			0,
			region.minY(),
			0,
			(region.getXSpan() >> lod) - 1,
			region.maxY(),
			(region.getZSpan() >> lod) - 1
		);
		return new ColumnBlockGetter(
			lists,
			columns,
			transformedVolume,
			transformedVolume, //not padded here.
			lod,
			this.dimensionType.hasSkyLight() ? (byte)(15) : (byte)(0),
			this.generatorParams.colors,
			this.generatorParams.biomeSource,
			this.dimensionType.cardinalLightType().get(),
			(ScriptedColumn[] toRecycle) -> this.columnRecycler.addAll(Arrays.asList(toRecycle))
		);
	}

	public T preload(BoundingBox area) {
		return null;
	}

	public QuadList loadOrGenerate(QuadColumn quadColumn, LoadMode mode, @Nullable T cache) {
		QuadList quadList = new QuadList();
		if (mode.canGenerate()) {
			quadList.createNew(quadColumn.object00.minY(), quadColumn.object00.maxY());
			QuadHolder.generate(quadColumn, quadList, this.generatorParams.layer.value());
		}
		return quadList;
	}

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

	public BlockSegmentList[] generateCaveCullingRegion(BoundingBox region) {
		int columnCount = region.getXSpan() * region.getZSpan();
		BlockSegmentList[] lists = new BlockSegmentList[columnCount];
		ScriptedColumn.Params params = new Params(
			this.generatorParams.columnSeed,
			0,
			0,
			region.minY(),
			region.maxY() + 1,
			ColumnUsage.HEIGHTMAP.builtinLodHints(0),
			this.generatorParams.compiledWorldTraits
		);
		Layer layer = this.generatorParams.layer.value();
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int z = region.minZ(); z <= region.maxZ(); z += 2) {
				final int z_ = z;
				for (int x = region.minX(); x <= region.maxX(); x += 2) {
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
						this.columnRecycler.addAll(Arrays.asList(quadColumn.object00, quadColumn.object01, quadColumn.object10, quadColumn.object11));
						int relativeX = (x_ - region.minX());
						int relativeZ = (z_ - region.minZ());
						int stride    =       region.getXSpan();
						int baseIndex = relativeZ * stride + relativeX;
						quadList.storeInArray(lists, baseIndex, stride);
					});
				}
			}
		}
		return lists;
	}

	@Override
	public void close() {

	}
}