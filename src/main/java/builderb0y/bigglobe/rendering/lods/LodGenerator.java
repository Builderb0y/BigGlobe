package builderb0y.bigglobe.rendering.lods;

import java.lang.ref.SoftReference;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.QuadHolder;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadColumn;
import builderb0y.bigglobe.chunkgen.QuadHolder.QuadList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.lods.LightweightChunk.ColumnIndexRange;
import builderb0y.bigglobe.util.*;
import builderb0y.bigglobe.rendering.lods.LodRenderer.MeshUploader;
import builderb0y.bigglobe.util.TimestampedComputingCache.ValueHolder;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.DirectionVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

@Environment(EnvType.CLIENT)
public class LodGenerator implements SafeCloseable {

	public static final int
		INNER_WIDTH           = 1 << LodQuadTree.MIN_LEVEL,
		INNER_AREA            = INNER_WIDTH * INNER_WIDTH,

		SINGLE_RENDER_PADDING = 2,
		DOUBLE_RENDER_PADDING = SINGLE_RENDER_PADDING << 1,
		RENDER_WIDTH          = INNER_WIDTH + DOUBLE_RENDER_PADDING,
		RENDER_AREA           = RENDER_WIDTH * RENDER_WIDTH,

		EDGE_THICKNESS        = 4,
		SHORT_EDGE_LENGTH     = INNER_WIDTH << 1,
		SHORT_EDGE_AREA       = SHORT_EDGE_LENGTH * EDGE_THICKNESS,
		LONG_EDGE_LENGTH      = SHORT_EDGE_LENGTH + EDGE_THICKNESS * 2,
		LONG_EDGE_AREA        = LONG_EDGE_LENGTH * EDGE_THICKNESS,
		EDGE_COLUMN_COUNT     = SHORT_EDGE_AREA * 2 + LONG_EDGE_AREA * 2,
		TOTAL_COLUMN_COUNT    = INNER_AREA + EDGE_COLUMN_COUNT;

	public static final ThreadLocal<Boolean>
		RENDERING_LODS = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public final LodSystem system;
	public final @Nullable LodChunkCache chunkCache;
	public final ClientGeneratorParams generatorParams;
	public final int maxLoadLevel;
	public final LinkedBlockingQueue<ScriptedColumn[]> columns;
	public final LinkedBlockingQueue<LodRequest> requests;
	public final ConcurrentLinkedQueue<LodSupply> currentSupply;
	public final AtomicInteger activeMeshers = new AtomicInteger();
	public final Thread thread;
	public volatile boolean running;
	public final byte topSkyLight;

	public String f3Message() {
		int
			loadOnly  = 0,
			loadOrGen = 0,
			genOnly   = 0,
			total     = 0;
		for (LodRequest request : this.requests) {
			switch (request.loadMode()) {
				case LOAD_ONLY        -> loadOnly++;
				case LOAD_OR_GENERATE -> loadOrGen++;
				case GENERATE_ONLY    -> genOnly++;
			}
			total++;
		}
		return "[BG] LOD Req L: " + loadOnly + ", G: " + genOnly + ", LG: " + loadOrGen + ", T: " + total + ", Cache: " + this.cacheDescription();
	}

	public String cacheDescription() {
		if (this.chunkCache == null) return "null";
		TimestampedComputingCache<ChunkPos, LightweightChunk> chunks = this.chunkCache.chunks;
		int present = chunks.presentCount.get();
		int total   = chunks.size();
		int empty   = total - present;
		return present + "/" + empty + "/" + total;
	}

	public LodGenerator(
		LodSystem system,
		ClientGeneratorParams generatorParams
	) {
		this.system = system;
		this.generatorParams = generatorParams;
		this.maxLoadLevel = BigGlobeConfig.INSTANCE.get().lodRendering.maxLodForChunkLoading;
		int threads = Runtime.getRuntime().availableProcessors();
		this.columns = new LinkedBlockingQueue<>(threads);
		ScriptedColumn.Factory factory = generatorParams.columnEntryRegistry.columnFactory;
		Params params = new Params(
			generatorParams.columnSeed,
			0,
			0,
			generatorParams.minY,
			generatorParams.maxY,
			ColumnUsage.RAW_GENERATION.builtinLodHints(0),
			generatorParams.compiledWorldTraits
		);
		for (int thread = 0; thread < threads; thread++) {
			ScriptedColumn[] columns = new ScriptedColumn[TOTAL_COLUMN_COUNT];
			for (int index = 0; index < TOTAL_COLUMN_COUNT; index++) {
				columns[index] = factory.create(params);
			}
			this.columns.add(columns);
		}
		this.requests = new LinkedBlockingQueue<>();
		this.currentSupply = new ConcurrentLinkedQueue<>();
		this.thread = new Thread(this::runLoop, "Big Globe LOD generator thread");

		LodChunkCache chunkCache = null;
		IntegratedServer server = MinecraftClient.getInstance().getServer();
		if (server != null) {
			ServerWorld serverWorld = server.getWorld(this.system.world.getRegistryKey());
			if (serverWorld != null) {
				chunkCache = new LodChunkCache(this, serverWorld);
			}
		}
		this.chunkCache = chunkCache;
		this.topSkyLight = system.world.getDimension().hasSkyLight() ? ((byte)(15)) : ((byte)(0));
	}

	public void start() {
		if (!this.running) {
			this.running = true;
			this.thread.start();
		}
	}

	@Override
	public void close() {
		this.running = false;
		try {
			this.thread.interrupt();
			this.thread.join();
		}
		catch (InterruptedException exception) {
			BigGlobeMod.LOGGER.warn("Who's trying to interrupt the shutdown process?", exception);
		}
		long nextLog = System.currentTimeMillis() + 5000L;
		for (int meshers; (meshers = this.activeMeshers.get()) > 0;) {
			Thread.onSpinWait();
			if (System.currentTimeMillis() >= nextLog) {
				BigGlobeMod.LOGGER.info("Waiting for " + meshers + " task(s) to complete...");
				nextLog += 5000L;
			}
		}
		ResourceTracker.closeAll(this.requests);
		ResourceTracker.closeAll(this.currentSupply);
		if (this.chunkCache != null) this.chunkCache.close();
	}

	public void request(LodQuadTree tree, LoadMode loadMode) {
		assert !tree.isQueued() : "attempt to request already-queued tree";
		this.requests.add(new LodRequest(this.system, tree, loadMode, this.system.renderer.beginMeshing()));
		tree.rebuildTime = Long.MAX_VALUE;
		tree.setQueued(true);
	}

	public boolean hasSupply() {
		return !this.currentSupply.isEmpty();
	}

	public @Nullable LodSupply getSupply() {
		return this.currentSupply.poll();
	}

	public void runLoop() {
		while (true) try {
			if (!this.running) {
				BigGlobeMod.LOGGER.info("Big Globe LOD generator thread shutting down.");
				break;
			}

			LodRequest request = null;
			try {
				request = this.requests.take();
			}
			catch (InterruptedException ignored) {}

			if (request != null) {
				if (request.owner.isQueued()) { //not thread-safe!
					this.buildRegion(request);
				}
				else {
					this.currentSupply.add(new LodSupply(request, false));
				}
			}
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Exception in Big Globe LOD generator thread:", throwable);
			this.running = false;
			break;
		}
	}

	public static record ColumnResults(
		ScriptedColumn[] recyclableColumns,
		ScriptedColumn[] worldColumns,
		BlockSegmentList[] lists
	) {}

	public void buildRegion(LodRequest request) {
		ColumnResults results = (
			request.owner.level == LodQuadTree.MIN_LEVEL
			? this.generateLod0Columns(request)
			: this.generateLodNColumns(request)
		);
		if (results == null) {
			this.currentSupply.add(new LodSupply(request, false));
			return;
		}
		this.activeMeshers.incrementAndGet();
		BigGlobeThreadPool.lodExecutor().execute(() -> {
			Boolean oldRenderingLods = RENDERING_LODS.get();
			try {
				RENDERING_LODS.set(Boolean.TRUE);
				this.buildGeometry(request, results, request.provider);
				this.currentSupply.add(new LodSupply(request, true));
			}
			catch (Throwable throwable) {
				this.currentSupply.add(new LodSupply(request, false));
				BigGlobeMod.LOGGER.error("Exception generating LOD meshes:", throwable);
				this.running = false;
			}
			finally {
				RENDERING_LODS.set(oldRenderingLods);
				this.columns.add(results.recyclableColumns);
				this.activeMeshers.decrementAndGet();
			}
		});
	}

	public BlockSegmentList[] generateCullingChunk(ChunkPos chunkPos) {
		ScriptedColumn[] columns;
		try {
			columns = this.columns.take();
		}
		catch (InterruptedException ignored) {
			return null;
		}
		try {
			BlockSegmentList[] lists = new BlockSegmentList[ColumnIndexRange.LOD4.end];
			int minY = this.generatorParams.minY;
			int maxY = this.generatorParams.maxY;
			Layer layer = this.generatorParams.layer.value();
			Params params = new Params(
				this.generatorParams.columnSeed,
				0,
				0,
				minY,
				maxY,
				ColumnUsage.HEIGHTMAP.builtinLodHints(0),
				this.generatorParams.compiledWorldTraits
			);
			int minX = chunkPos.x << 4;
			int minZ = chunkPos.z << 4;
			try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
				for (int offsetZ = 0; offsetZ < 16; offsetZ += 2) {
					final int offsetZ_ = offsetZ;
					for (int offsetX = 0; offsetX < 16; offsetX += 2) {
						final int offsetX_ = offsetX;
						async.submit(() -> {
							int baseIndex = (offsetZ_ << 4) | offsetX_;
							int quadX = minX | offsetX_;
							int quadZ = minZ | offsetZ_;
							QuadColumn quadColumn = new QuadColumn();
							quadColumn.loadFromArray(columns, baseIndex, 16);
							quadColumn.at(params, quadX, quadZ, 1);
							QuadList quadList = new QuadList();
							quadList.createNew(minY, maxY);
							QuadHolder.generate(quadColumn, quadList, layer);
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
		finally {
			this.columns.add(columns);
		}
	}

	public static boolean anyNonNulls(LightweightChunk[] chunks) {
		for (LightweightChunk chunk : chunks) {
			if (chunk != null) return true;
		}
		return false;
	}

	public @Nullable ColumnResults generateLod0Columns(LodRequest request) {
		int minX = request.owner.minX();
		int minZ = request.owner.minZ();
		int maxX = request.owner.maxX();
		int maxZ = request.owner.maxZ();
		int paddedMinX = minX - 2;
		int paddedMinZ = minZ - 2;
		int paddedMaxX = maxX + 2;
		int paddedMaxZ = maxZ + 2;
		ScriptedColumn[] columns;
		try {
			columns = this.columns.take();
		}
		catch (InterruptedException ignored) {
			return null;
		}
		BlockSegmentList[] lists = new BlockSegmentList[RENDER_AREA];
		int minY = this.generatorParams.minY;
		int maxY = this.generatorParams.maxY;
		Layer layer = this.generatorParams.layer.value();
		Params params = new Params(
			this.generatorParams.columnSeed,
			0,
			0,
			minY,
			maxY,
			ColumnUsage.RAW_GENERATION.builtinLodHints(0),
			this.generatorParams.compiledWorldTraits
		);
		if (request.loadMode.canLoad() && this.chunkCache != null && this.maxLoadLevel > 0) {
			int chunkMinX =   paddedMinX      >> 4;
			int chunkMinZ =   paddedMinZ      >> 4;
			int chunkMaxX = ((paddedMaxX - 1) >> 4) + 1;
			int chunkMaxZ = ((paddedMaxZ - 1) >> 4) + 1;
			LightweightChunk[] chunks = this.chunkCache.getChunks(
				new ChunkPos(chunkMinX, chunkMinZ),
				new ChunkPos(chunkMaxX, chunkMaxZ)
			);
			if (anyNonNulls(chunks)) {
				for (int dstZ = paddedMinZ; dstZ < paddedMaxZ; dstZ++) {
					for (int dstX = paddedMinX; dstX < paddedMaxX; dstX++) {
						int chunkX = (dstX >> 4) - chunkMinX;
						int chunkZ = (dstZ >> 4) - chunkMinZ;
						int chunkIndex = chunkZ * (chunkMaxX - chunkMinX) + chunkX;
						LightweightChunk chunk = chunks[chunkIndex];
						if (chunk != null) {
							int listIndex = (dstZ - paddedMinZ) * RENDER_WIDTH + (dstX - paddedMinX);
							lists[listIndex] = chunk.getColumn(dstX, dstZ, 0);
						}
					}
				}
			}
			else {
				if (!request.loadMode.canGenerate()) {
					this.columns.add(columns);
					return null;
				}
			}
		}
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int quadZ = paddedMinZ; quadZ < paddedMaxZ; quadZ += 2) {
				for (int quadX = paddedMinX; quadX < paddedMaxX; quadX += 2) {
					int baseIndex = (quadZ - paddedMinZ) * RENDER_WIDTH + (quadX - paddedMinX);
					QuadColumn quadColumn = new QuadColumn();
					quadColumn.loadFromArray(columns, baseIndex, RENDER_WIDTH);
					quadColumn.at(params, quadX, quadZ, 1);
					QuadList quadList = new QuadList();
					quadList.loadFromArray(lists, baseIndex, RENDER_WIDTH);
					if (quadList.anyNull()) async.submit(() -> {
						quadList.createNew(minY, maxY);
						QuadHolder.generate(quadColumn, quadList, layer);
						quadList.computeLightLevels(LodGenerator.this.topSkyLight);
						quadList.storeInArray(lists, baseIndex, RENDER_WIDTH);
					});
				}
			}
		}
		return new ColumnResults(columns, columns, lists);
	}

	public @Nullable ColumnResults generateLodNColumns(LodRequest request) {
		int lod  = request.owner.level;
		int blockLod = lod - LodQuadTree.MIN_LEVEL;
		int innerStep = 1 << blockLod;
		int innerQuadSize = innerStep << 1;
		int outerStep = innerStep >> 1;
		int outerQuadSize = innerQuadSize >> 1;
		int minX = request.owner.minX();
		int minZ = request.owner.minZ();
		int maxX = request.owner.maxX();
		int maxZ = request.owner.maxZ();
		ScriptedColumn[] columns;
		try {
			columns = this.columns.take();
		}
		catch (InterruptedException ignored) {
			return null;
		}
		BlockSegmentList[] lists = new BlockSegmentList[RENDER_AREA];
		ScriptedColumn[] worldColumns = new ScriptedColumn[RENDER_AREA];
		int minY = this.generatorParams.minY;
		int maxY = this.generatorParams.maxY;
		Layer layer = this.generatorParams.layer.value();
		Params params = new Params(
			this.generatorParams.columnSeed,
			0,
			0,
			this.generatorParams.minY,
			this.generatorParams.maxY,
			ColumnUsage.RAW_GENERATION.builtinLodHints(blockLod),
			this.generatorParams.compiledWorldTraits
		);
		int paddedMinX = minX - outerStep * EDGE_THICKNESS;
		int paddedMinZ = minZ - outerStep * EDGE_THICKNESS;
		int paddedMaxX = maxX + outerStep * EDGE_THICKNESS;
		int paddedMaxZ = maxZ + outerStep * EDGE_THICKNESS;
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			abstract class BaseHelper {

				public abstract void addCenter(int quadX, int quadZ);

				public void generateCenter(QuadColumn quadColumn, int listIndex) {
					async.submit(() -> {
						QuadList quadList = new QuadList();
						quadList.createNew(minY, maxY);
						QuadHolder.generate(quadColumn, quadList, layer);
						quadList.downscale(blockLod);
						quadList.computeLightLevels(LodGenerator.this.topSkyLight);
						quadList.storeInArray(lists, listIndex, RENDER_WIDTH);
					});
				}

				public QuadColumn transferCenterColumns(int quadX, int quadZ, int columnIndex, int listIndex) {
					QuadColumn quadColumn = new QuadColumn();
					quadColumn.loadFromArray(columns, columnIndex, INNER_WIDTH);
					quadColumn.at(params, quadX, quadZ, innerStep);
					quadColumn.storeInArray(worldColumns, listIndex, RENDER_WIDTH);
					return quadColumn;
				}

				public abstract void addEdge(int quadX, int quadZ, int columnIndex);

				public void generateEdge(QuadColumn quadColumn, int listIndex) {
					async.submit(() -> {
						QuadList quadList = new QuadList();
						quadList.createNew(minY, maxY);
						QuadHolder.generate(quadColumn, quadList, layer);
						BlockSegmentList merged = quadList.merge();
						merged = QuadList.downscaleColumnKeepAir(merged, blockLod);
						merged.computeLightLevels(LodGenerator.this.topSkyLight);
						lists[listIndex] = merged;
					});
				}

				public QuadColumn transferEdgeColumn(int quadX, int quadZ, int columnIndex, int listIndex) {
					QuadColumn quadColumn = new QuadColumn();
					quadColumn.loadFromArray(columns, columnIndex, 2);
					quadColumn.at(params, quadX, quadZ, outerStep);
					worldColumns[listIndex] = quadColumn.object00;
					return quadColumn;
				}

				public int columnDestinationIndex(int blockX, int blockZ) {
					int relativeX = (blockX - paddedMinX) >> blockLod;
					int relativeZ = (blockZ - paddedMinZ) >> blockLod;
					return relativeZ * RENDER_WIDTH + relativeX;
				}

				public int columnSourceIndex(int blockX, int blockZ) {
					int relativeX = (blockX - minX) >> blockLod;
					int relativeZ = (blockZ - minZ) >> blockLod;
					return relativeZ * INNER_WIDTH + relativeX;
				}
			}
			class NonLoadingHelper extends BaseHelper {

				@Override
				public void addCenter(int quadX, int quadZ) {
					int columnIndex = this.columnSourceIndex(quadX, quadZ);
					int listIndex = this.columnDestinationIndex(quadX, quadZ);
					QuadColumn quadColumn = this.transferCenterColumns(quadX, quadZ, columnIndex, listIndex);
					this.generateCenter(quadColumn, listIndex);
				}

				@Override
				public void addEdge(int quadX, int quadZ, int columnIndex) {
					int listIndex = this.columnDestinationIndex(quadX, quadZ);
					QuadColumn quadColumn = this.transferEdgeColumn(quadX, quadZ, columnIndex, listIndex);
					this.generateEdge(quadColumn, listIndex);
				}
			}
			BaseHelper helper;
			if (request.loadMode.canLoad() && this.chunkCache != null && this.maxLoadLevel > blockLod) {
				int chunkMinX =   paddedMinX      >> 4;
				int chunkMinZ =   paddedMinZ      >> 4;
				int chunkMaxX = ((paddedMaxX - 1) >> 4) + 1;
				int chunkMaxZ = ((paddedMaxZ - 1) >> 4) + 1;
				LightweightChunk[] chunks = this.chunkCache.getChunks(
					new ChunkPos(chunkMinX, chunkMinZ),
					new ChunkPos(chunkMaxX, chunkMaxZ)
				);
				class LoadingHelper extends BaseHelper {

					public @Nullable BlockSegmentList obtainList(int blockX, int blockZ, boolean edge) {
						int chunkX = (blockX >> 4) - chunkMinX;
						int chunkZ = (blockZ >> 4) - chunkMinZ;
						int chunkIndex = chunkZ * (chunkMaxX - chunkMinX) + chunkX;
						LightweightChunk chunk = chunks[chunkIndex];
						if (chunk != null) {
							int lod = blockLod - (edge ? 1 : 0);
							return chunk.getColumn(blockX >> lod, blockZ >> lod, lod);
						}
						else {
							return null;
						}
					}

					public @Nullable QuadList getExistingLists(int quadX, int quadZ, boolean edge) {
						int step = edge ? outerStep : innerStep;
						BlockSegmentList list00 = this.obtainList(quadX,        quadZ,        edge);
						if (list00 == null) return null;
						BlockSegmentList list01 = this.obtainList(quadX + step, quadZ,        edge);
						if (list01 == null) return null;
						BlockSegmentList list10 = this.obtainList(quadX, quadZ + step, edge);
						if (list10 == null) return null;
						BlockSegmentList list11 = this.obtainList(quadX + step, quadZ + step, edge);
						if (list11 == null) return null;

						return new QuadList(list00, list01, list10, list11);
					}

					@Override
					public void addCenter(int quadX, int quadZ) {
						int columnIndex = this.columnSourceIndex(quadX, quadZ);
						int listIndex = this.columnDestinationIndex(quadX, quadZ);
						QuadColumn quadColumn = this.transferCenterColumns(quadX, quadZ, columnIndex, listIndex);
						QuadList existing = this.getExistingLists(quadX, quadZ, false);
						if (existing != null) {
							existing.storeInArray(lists, listIndex, RENDER_WIDTH);
						}
						else {
							this.generateCenter(quadColumn, listIndex);
						}
					}

					@Override
					public void addEdge(int quadX, int quadZ, int columnIndex) {
						int listIndex = this.columnDestinationIndex(quadX, quadZ);
						QuadColumn quadColumn = this.transferEdgeColumn(quadX, quadZ, columnIndex, listIndex);
						QuadList existing = this.getExistingLists(quadX, quadZ, true);
						if (existing != null) {
							lists[listIndex] = QuadList.downscaleColumnKeepAir(existing.merge(), 1);
						}
						else {
							this.generateEdge(quadColumn, listIndex);
						}
					}
				}
				if (anyNonNulls(chunks)) {
					helper = new LoadingHelper();
				}
				else if (request.loadMode.canGenerate()) {
					helper = new NonLoadingHelper();
				}
				else {
					this.columns.add(columns);
					return null;
				}
			}
			else {
				helper = new NonLoadingHelper();
			}
			for (int dstZ = minZ; dstZ < maxZ; dstZ += innerQuadSize) {
				for (int dstX = minX; dstX < maxX; dstX += innerQuadSize) {
					helper.addCenter(dstX, dstZ);
				}
			}
			int columnIndex = INNER_AREA;
			for (int quadX = paddedMinX; quadX < paddedMaxX; quadX += outerQuadSize) {
				for (int quadZ = paddedMinZ; quadZ < minZ; quadZ += outerQuadSize) {
					helper.addEdge(quadX, quadZ, columnIndex);
					columnIndex += 4;
				}
				for (int quadZ = maxZ; quadZ < paddedMaxZ; quadZ += outerQuadSize) {
					helper.addEdge(quadX, quadZ, columnIndex);
					columnIndex += 4;
				}
			}
			for (int quadZ = minZ; quadZ < maxZ; quadZ += outerQuadSize) {
				for (int quadX = paddedMinX; quadX < minX; quadX += outerQuadSize) {
					helper.addEdge(quadX, quadZ, columnIndex);
					columnIndex += 4;
				}
				for (int quadX = maxX; quadX < paddedMaxX; quadX += outerQuadSize) {
					helper.addEdge(quadX, quadZ, columnIndex);
					columnIndex += 4;
				}
			}
		}
		return new ColumnResults(columns, worldColumns, lists);
	}

	public static boolean quickCheckRender(BlockState self, BlockState other) {
		if (BlockStateVersions.isOpaqueFullCube(other, EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) return false;
		FluidState fluid = self.getFluidState();
		return fluid.getBlockState() != self /* false for waterlogged blocks */ || other.getFluidState() != fluid;
	}

	public void buildGeometry(
		LodRequest request,
		ColumnResults results,
		VersionedVertexConsumerProvider provider
	) {
		BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
		BlockModelRenderer renderer = blockRenderManager.getModelRenderer();
		BlockSegmentList[] lists = results.lists;
		ColumnBlockView columnBlockView = new ColumnBlockView(
			this.system.world,
			lists,
			request.owner.minX(),
			request.owner.minZ(),
			request.owner.level,
			results.worldColumns,
			this.generatorParams
		);
		BlockSegmentList[] adjacents = new BlockSegmentList[4];
		BlockPos.Mutable pos = new BlockPos.Mutable();
		MatrixStack matrixStack = new MatrixStack();
		for (pos.setZ(0); pos.getZ() < INNER_WIDTH; pos.setZ(pos.getZ() + 1)) {
			for (pos.setX(0); pos.getX() < INNER_WIDTH; pos.setX(pos.getX() + 1)) {
				int baseColumnIndex = (pos.getZ() + SINGLE_RENDER_PADDING) * RENDER_WIDTH + (pos.getX() + SINGLE_RENDER_PADDING);
				BlockSegmentList center = lists[baseColumnIndex];
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_X)] = lists[baseColumnIndex + 1];
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_X)] = lists[baseColumnIndex - 1];
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_Z)] = lists[baseColumnIndex + RENDER_WIDTH];
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_Z)] = lists[baseColumnIndex - RENDER_WIDTH];
				for (int centerIndex = 0, centerSize = center.size(); centerIndex < centerSize; centerIndex++) {
					if (center.size() != centerSize) {
						throw new ConcurrentModificationException();
					}
					LitSegment centerSegment = center.get(centerIndex);
					if (!centerSegment.value.isAir()) {
						for (pos.setY(centerSegment.minY); pos.getY() <= centerSegment.maxY;) {
							int y = pos.getY();
							int nextY;
							boolean shouldRender;
							if (y == centerSegment.minY && centerIndex - 1 >= 0 && quickCheckRender(centerSegment.value, center.get(centerIndex - 1).value)) {
								shouldRender = true;
								nextY = y + 1;
							}
							else if (y == centerSegment.maxY && centerIndex + 1 < centerSize && quickCheckRender(centerSegment.value, center.get(centerIndex + 1).value)) {
								shouldRender = true;
								nextY = y + 1;
							}
							else {
								shouldRender = false;
								int skipTo = centerSegment.maxY;
								for (Direction direction : Directions.HORIZONTAL) {
									BlockSegmentList adjacent = adjacents[DirectionVersions.horizontal(direction)];
									LitSegment adjacentSegment = adjacent.getOverlappingSegment(y);
									if (adjacentSegment == null || quickCheckRender(centerSegment.value, adjacentSegment.value)) {
										shouldRender = true;
										skipTo = y + 1;
										break;
									}
									else {
										skipTo = Math.min(skipTo, adjacentSegment.maxY + 1);
									}
								}
								nextY = Math.max(skipTo, y + 1);
							}
							if (shouldRender) {
								matrixStack.push();
								matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
								#if MC_VERSION >= MC_1_21_5
									renderer.render(
										columnBlockView,
										blockRenderManager.getModel(centerSegment.value),
										centerSegment.value,
										pos,
										matrixStack,
										provider,
										true,
										centerSegment.value.getRenderingSeed(pos),
										OverlayTexture.DEFAULT_UV
									);
								#else
									renderer.render(
										columnBlockView,
										blockRenderManager.getModel(centerSegment.value),
										centerSegment.value,
										pos,
										matrixStack,
										provider.getBuffer(RenderLayers.getBlockLayer(centerSegment.value)),
										true,
										Random.create(),
										centerSegment.value.getRenderingSeed(pos),
										OverlayTexture.DEFAULT_UV
									);
								#endif
								FluidState fluidState = centerSegment.value.getFluidState();
								if (!fluidState.isEmpty()) {
									FluidRenderHandler fabricHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getFluid());
									if (fabricHandler != null) {
										fabricHandler.renderFluid(
											pos,
											columnBlockView,
											provider.getBuffer(layerOf(fluidState)),
											centerSegment.value,
											fluidState
										);
									}
									else {
										blockRenderManager.renderFluid(
											pos,
											columnBlockView,
											provider.getBuffer(layerOf(fluidState)),
											centerSegment.value,
											fluidState
										);
									}
								}
								matrixStack.pop();
							}
							pos.setY(nextY);
						}
					}
				}
			}
		}
	}

	#if MC_VERSION >= MC_1_21_11

		public static BlockRenderLayer layerOf(FluidState state) {
			return BlockRenderLayers.getFluidLayer(state);
		}

	#elif MC_VERSION >= MC_1_21_8

		public static BlockRenderLayer layerOf(FluidState state) {
			return RenderLayers.getFluidLayer(state);
		}

	#else

		public static RenderLayer layerOf(FluidState state) {
			return RenderLayers.getFluidLayer(state);
		}

	#endif

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
	}

	public static record LodRequest(
		LodSystem system,
		LodQuadTree owner,
		LoadMode loadMode,
		VersionedVertexConsumerProvider provider
	)
	implements SafeCloseable {

		@Override
		public void close() {
			this.system.renderer.endMeshing(this.provider);
		}
	}

	public static record LodSupply(
		LodRequest request,
		boolean success
	)
	implements SafeCloseable {

		public void apply(MeshUploader uploader, int maxLoadLevel) {
			LodQuadTree owner = this.request.owner;
			try {
				if (owner.isQueued()) {
					if (this.success) {
						if (owner.passes != null) {
							owner.passes.close();
							owner.passes = null;
						}
						else if (owner.level - LodQuadTree.MIN_LEVEL < maxLoadLevel) {
							owner.rebuildTime = System.currentTimeMillis() + LodSystem.CHUNK_REBUILD_DELAY;
						}
						owner.passes = uploader.upload(this.request.provider);
					}
					owner.setQueued(false);
				}
			}
			catch (Throwable throwable) {
				if (owner.isQueued()) {
					owner.setQueued(false); //try again later.
				}
				throw AutoCodecUtil.rethrow(throwable);
			}
			finally {
				this.close();
			}
		}

		@Override
		public void close() {
			this.request.close();
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ColumnBlockView implements BlockRenderView {

		public final ClientGeneratorParams generator;
		public final ClientWorld delegate;
		public final BlockSegmentList[] lists;
		public final int minX, minZ, lod;
		public final ScriptedColumn[] columns;
		public final BlockPos.Mutable colorGetter;
		public RegistryEntry<Biome> plainsBiome;
		public LightingProvider lightingProvider;

		public ColumnBlockView(
			ClientWorld delegate,
			BlockSegmentList[] lists,
			int minX,
			int minZ,
			int lod,
			ScriptedColumn[] columns,
			ClientGeneratorParams generator
		) {
			this.delegate = delegate;
			this.generator = generator;
			this.lists = lists;
			this.minX = minX;
			this.minZ = minZ;
			this.lod = lod;
			this.columns = columns;
			this.colorGetter = new BlockPos.Mutable();
		}

		public ScriptedColumn getColumn(BlockPos pos) {
			int x = Objects.checkIndex(pos.getX() + SINGLE_RENDER_PADDING, RENDER_WIDTH);
			int z = Objects.checkIndex(pos.getZ() + SINGLE_RENDER_PADDING, RENDER_WIDTH);
			return Objects.requireNonNull(this.columns[z * RENDER_WIDTH + x]);
		}

		@Override
		public float getBrightness(Direction direction, boolean shaded) {
			return this.delegate.getBrightness(direction, shaded);
		}

		@Override
		public LightingProvider getLightingProvider() {
			LightingProvider provider = this.lightingProvider;
			if (provider == null) {
				provider = this.lightingProvider = new LightingProvider(this.delegate.getChunkManager(), false, false);
			}
			return provider;
		}

		@Override
		public int getColor(BlockPos pos, ColorResolver colorResolver) {
			int y = pos.getY() << (this.lod - LodQuadTree.MIN_LEVEL);
			ScriptedColumn column = this.getColumn(pos);
			if (colorResolver == BiomeColors.GRASS_COLOR) {
				if (this.generator.grassColor != null) {
					return this.generator.grassColor.getColor(column, y);
				}
			}
			else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
				if (this.generator.foliageColor != null) {
					return this.generator.foliageColor.getColor(column, y);
				}
			}
			else if (colorResolver == BiomeColors.WATER_COLOR) {
				if (this.generator.waterColor != null) {
					return this.generator.waterColor.getColor(column, y);
				}
			}
			if (this.plainsBiome == null) {
				this.plainsBiome = RegistryVersions.getEntry(this.delegate.getRegistryManager(), BiomeKeys.PLAINS);
			}
			return colorResolver.getColor(this.plainsBiome.value(), column.x(), column.z());
		}

		@Override
		public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
			return null;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			int x = pos.getX() + SINGLE_RENDER_PADDING;
			if (x < 0 || x >= RENDER_WIDTH) return BlockStates.AIR;
			int z = pos.getZ() + SINGLE_RENDER_PADDING;
			if (z < 0 || z >= RENDER_WIDTH) return BlockStates.AIR;
			BlockState state = this.lists[z * RENDER_WIDTH + x].getBlockState(pos.getY());
			return state != null ? state : Blocks.AIR.getDefaultState();
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return this.getBlockState(pos).getFluidState();
		}

		@Override
		public int getHeight() {
			return this.lists[0].maxY() - this.lists[0].minY();
		}

		@Override
		public int getBottomY() {
			return this.lists[0].minY();
		}

		#if MC_VERSION >= MC_1_21_2

			@Override
			public int getTopYInclusive() {
				return this.lists[0].maxY;
			}

		#else

			@Override
			public int getTopY() {
				return this.lists[0].maxY();
			}

		#endif

		@Override
		public int getLightLevel(LightType type, BlockPos pos) {
			int x = pos.getX() + SINGLE_RENDER_PADDING;
			if (x < 0 || x >= RENDER_WIDTH) return 15;
			int z = pos.getZ() + SINGLE_RENDER_PADDING;
			if (z < 0 || z >= RENDER_WIDTH) return 15;
			LitSegment segment = this.lists[z * RENDER_WIDTH + x].getOverlappingSegment(pos.getY());
			return segment == null ? 15 : switch (type) {
				case BLOCK -> segment.getBlockLight();
				case SKY -> segment.getLightLevel(pos.getY(), this.lod - LodQuadTree.MIN_LEVEL);
			};
		}
	}
}