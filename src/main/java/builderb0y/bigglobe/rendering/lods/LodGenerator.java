package builderb0y.bigglobe.rendering.lods;

import java.util.Objects;
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
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
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
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList;
import builderb0y.bigglobe.chunkgen.scripted.BlockSegmentList.LitSegment;
import builderb0y.bigglobe.chunkgen.scripted.Layer;
import builderb0y.bigglobe.chunkgen.scripted.SegmentList.Segment;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.rendering.ResourceTracker;
import builderb0y.bigglobe.rendering.SafeCloseable;
import builderb0y.bigglobe.rendering.lods.LodRenderer.MeshUploader;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.Directions;
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
	public final ClientGeneratorParams generatorParams;
	public final LinkedBlockingQueue<ScriptedColumn[]> columns;
	public final Thread thread;
	public volatile boolean running;
	public final LinkedBlockingQueue<LodRequest> requests;
	public final ConcurrentLinkedQueue<LodSupply> currentSupply;
	public final AtomicInteger activeMeshers = new AtomicInteger();

	public LodGenerator(
		LodSystem system,
		ClientGeneratorParams generatorParams
	) {
		this.system = system;
		this.generatorParams = generatorParams;
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
	}

	public void request(LodQuadTree tree) {
		assert !tree.isQueued() : "attempt to request already-queued tree";
		this.requests.add(new LodRequest(this.system, tree, this.system.renderer.beginMeshing()));
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
		if (results == null) return;
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

	public @Nullable ColumnResults generateLod0Columns(LodRequest request) {
		int lod  = request.owner.level;
		int blockLod = lod - LodQuadTree.MIN_LEVEL;
		int step = 1 << blockLod;
		int quadSize = step << 1;
		int minX = request.owner.minX() - quadSize;
		int minZ = request.owner.minZ() - quadSize;
		int maxX = request.owner.maxX() + quadSize;
		int maxZ = request.owner.maxZ() + quadSize;
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
			this.generatorParams.minY,
			this.generatorParams.maxY,
			ColumnUsage.RAW_GENERATION.builtinLodHints(blockLod),
			this.generatorParams.compiledWorldTraits
		);
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int quadZ = minZ; quadZ < maxZ; quadZ += quadSize) {
				final int quadZ_ = quadZ;
				for (int quadX = minX; quadX < maxX; quadX += quadSize) {
					final int quadX_ = quadX;
					async.submit(() -> {
						int baseIndex = ((quadZ_ - minZ) >> blockLod) * RENDER_WIDTH + ((quadX_ - minX) >> blockLod);
						ScriptedColumn
							column00 = columns[baseIndex],
							column01 = columns[baseIndex + 1],
							column10 = columns[baseIndex + RENDER_WIDTH],
							column11 = columns[baseIndex + (RENDER_WIDTH + 1)];
						column00.setParamsUnchecked(params.at(quadX_,        quadZ_       ));
						column01.setParamsUnchecked(params.at(quadX_ + step, quadZ_       ));
						column10.setParamsUnchecked(params.at(quadX_,        quadZ_ + step));
						column11.setParamsUnchecked(params.at(quadX_ + step, quadZ_ + step));
						BlockSegmentList
							list00 = new BlockSegmentList(minY, maxY),
							list01 = new BlockSegmentList(minY, maxY),
							list10 = new BlockSegmentList(minY, maxY),
							list11 = new BlockSegmentList(minY, maxY);
						layer.emitSegments(column00, column01, column10, column11, list00);
						layer.emitSegments(column01, column00, column11, column10, list01);
						layer.emitSegments(column10, column11, column00, column01, list10);
						layer.emitSegments(column11, column10, column01, column00, list11);
						list00.computeLightLevels();
						list01.computeLightLevels();
						list10.computeLightLevels();
						list11.computeLightLevels();
						lists[baseIndex] = list00;
						lists[baseIndex + 1] = list01;
						lists[baseIndex + RENDER_WIDTH] = list10;
						lists[baseIndex + (RENDER_WIDTH + 1)] = list11;
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
		try (AsyncRunner async = BigGlobeThreadPool.lodRunner()) {
			for (int quadZ = minZ; quadZ < maxZ; quadZ += innerQuadSize) {
				final int quadZ_ = quadZ;
				for (int quadX = minX; quadX < maxX; quadX += innerQuadSize) {
					final int quadX_ = quadX;
					async.submit(() -> {
						int relativeX = (quadX_ - minX) >> blockLod;
						int relativeZ = (quadZ_ - minZ) >> blockLod;
						int baseIndex = relativeZ * INNER_WIDTH + relativeX;
						ScriptedColumn
							column00 = columns[baseIndex],
							column01 = columns[baseIndex + 1],
							column10 = columns[baseIndex + INNER_WIDTH],
							column11 = columns[baseIndex + (INNER_WIDTH + 1)];
						column00.setParamsUnchecked(params.at(quadX_,        quadZ_       ));
						column01.setParamsUnchecked(params.at(quadX_ + innerStep, quadZ_       ));
						column10.setParamsUnchecked(params.at(quadX_,        quadZ_ + innerStep));
						column11.setParamsUnchecked(params.at(quadX_ + innerStep, quadZ_ + innerStep));
						BlockSegmentList
							list00 = new BlockSegmentList(minY, maxY),
							list01 = new BlockSegmentList(minY, maxY),
							list10 = new BlockSegmentList(minY, maxY),
							list11 = new BlockSegmentList(minY, maxY);
						layer.emitSegments(column00, column01, column10, column11, list00);
						layer.emitSegments(column01, column00, column11, column10, list01);
						layer.emitSegments(column10, column11, column00, column01, list10);
						layer.emitSegments(column11, column10, column01, column00, list11);
						if (blockLod > 0) {
							list00 = this.downsampleColumn(list00, blockLod);
							list01 = this.downsampleColumn(list01, blockLod);
							list10 = this.downsampleColumn(list10, blockLod);
							list11 = this.downsampleColumn(list11, blockLod);
						}
						list00.computeLightLevels();
						list01.computeLightLevels();
						list10.computeLightLevels();
						list11.computeLightLevels();
						baseIndex = (relativeZ + SINGLE_RENDER_PADDING) * RENDER_WIDTH + (relativeX + SINGLE_RENDER_PADDING);
						lists[baseIndex] = list00;
						lists[baseIndex + 1] = list01;
						lists[baseIndex + RENDER_WIDTH] = list10;
						lists[baseIndex + (RENDER_WIDTH + 1)] = list11;
						worldColumns[baseIndex] = column00;
						worldColumns[baseIndex + 1] = column01;
						worldColumns[baseIndex + RENDER_WIDTH] = column10;
						worldColumns[baseIndex + (RENDER_WIDTH + 1)] = column11;
					});
				}
			}
			class EdgeTask implements Runnable {

				public final int quadX, quadZ, columnIndex, listIndex;

				public EdgeTask(int quadX, int quadZ, int columnIndex, int listIndex) {
					if (columnIndex + 3 >= TOTAL_COLUMN_COUNT) {
						throw new IllegalArgumentException();
					}
					if (listIndex >= RENDER_AREA) {
						throw new IllegalArgumentException();
					}
					this.quadX = quadX;
					this.quadZ = quadZ;
					this.columnIndex = columnIndex;
					this.listIndex = listIndex;
				}

				@Override
				public void run() {
					ScriptedColumn
						column00 = columns[this.columnIndex],
						column01 = columns[this.columnIndex + 1],
						column10 = columns[this.columnIndex + 2],
						column11 = columns[this.columnIndex + 3];
					column00.setParamsUnchecked(params.at(this.quadX, this.quadZ));
					column01.setParamsUnchecked(params.at(this.quadX + outerStep, this.quadZ));
					column10.setParamsUnchecked(params.at(this.quadX, this.quadZ + outerStep));
					column11.setParamsUnchecked(params.at(this.quadX + outerStep, this.quadZ + outerStep));
					BlockSegmentList
						list00 = new BlockSegmentList(minY, maxY),
						list01 = new BlockSegmentList(minY, maxY),
						list10 = new BlockSegmentList(minY, maxY),
						list11 = new BlockSegmentList(minY, maxY);
					layer.emitSegments(column00, column01, column10, column11, list00);
					layer.emitSegments(column01, column00, column11, column10, list01);
					layer.emitSegments(column10, column11, column00, column01, list10);
					layer.emitSegments(column11, column10, column01, column00, list11);
					BlockSegmentList merged = LodGenerator.this.merge(list00, list01, list10, list11);
					merged = LodGenerator.this.downsampleColumnKeepAir(merged, blockLod);
					merged.computeLightLevels();
					lists[this.listIndex] = merged;
					worldColumns[this.listIndex] = column00;
				}
			}
			int columnIndex = INNER_AREA;
			int paddedMinX = minX - outerStep * EDGE_THICKNESS;
			int paddedMinZ = minZ - outerStep * EDGE_THICKNESS;
			int paddedMaxX = maxX + outerStep * EDGE_THICKNESS;
			int paddedMaxZ = maxZ + outerStep * EDGE_THICKNESS;
			for (int quadX = paddedMinX; quadX < paddedMaxX; quadX += outerQuadSize) {
				for (int quadZ = paddedMinZ; quadZ < minZ; quadZ += outerQuadSize) {
					async.submit(new EdgeTask(quadX, quadZ, columnIndex, ((quadZ - paddedMinZ) >> blockLod) * RENDER_WIDTH + ((quadX - paddedMinX) >> blockLod)));
					columnIndex += 4;
				}
				for (int quadZ = maxZ; quadZ < paddedMaxZ; quadZ += outerQuadSize) {
					async.submit(new EdgeTask(quadX, quadZ, columnIndex, ((quadZ - paddedMinZ) >> blockLod) * RENDER_WIDTH + ((quadX - paddedMinX) >> blockLod)));
					columnIndex += 4;
				}
			}
			for (int quadZ = minZ; quadZ < maxZ; quadZ += outerQuadSize) {
				for (int quadX = paddedMinX; quadX < minX; quadX += outerQuadSize) {
					async.submit(new EdgeTask(quadX, quadZ, columnIndex, ((quadZ - paddedMinZ) >> blockLod) * RENDER_WIDTH + ((quadX - paddedMinX) >> blockLod)));
					columnIndex += 4;
				}
				for (int quadX = maxX; quadX < paddedMaxX; quadX += outerQuadSize) {
					async.submit(new EdgeTask(quadX, quadZ, columnIndex, ((quadZ - paddedMinZ) >> blockLod) * RENDER_WIDTH + ((quadX - paddedMinX) >> blockLod)));
					columnIndex += 4;
				}
			}
			assert columnIndex == TOTAL_COLUMN_COUNT;
		}
		return new ColumnResults(columns, worldColumns, lists);
	}

	public BlockSegmentList merge(
		BlockSegmentList list00,
		BlockSegmentList list01,
		BlockSegmentList list10,
		BlockSegmentList list11
	) {
		BlockSegmentList result = new BlockSegmentList(list00.minY(), list00.maxY());
		result.addAllSegments(list00);
		this.copyAir(list01, list00);
		this.copyAir(list10, list00);
		this.copyAir(list11, list00);
		return result;
	}

	public void copyAir(BlockSegmentList source, BlockSegmentList destination) {
		for (Segment<BlockState> segment : source) {
			if (BlockStateVersions.getCullingShape(segment.value, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) != VoxelShapes.fullCube()) {
				destination.addSegment(segment.minY, segment.maxY, segment.value);
			}
		}
	}

	public BlockSegmentList downsampleColumnKeepAir(BlockSegmentList list, int lod) {
		BlockSegmentList newList = new BlockSegmentList(list.minY >> lod, (list.maxY >> lod) + 1);
		for (Segment<BlockState> segment : list) {
			int minY = segment.minY >> lod, maxY = segment.maxY >> lod;
			//ensure culling blocks can't overwrite non-culling blocks.
			if (BlockStateVersions.getCullingShape(segment.value, EmptyBlockView.INSTANCE, BlockPos.ORIGIN) == VoxelShapes.fullCube()) {
				Segment<BlockState> existing = newList.getOverlappingSegment(minY);
				if (existing != null) {
					minY = Math.max(minY, existing.maxY + 1);
				}
			}
			newList.addSegment(minY, maxY, segment.value);
		}
		return newList;
	}

	public BlockSegmentList downsampleColumn(BlockSegmentList list, int lod) {
		BlockSegmentList newList = new BlockSegmentList(list.minY >> lod, (list.maxY >> lod) + 1);
		for (Segment<BlockState> segment : list) {
			int minY = segment.minY >> lod, maxY = segment.maxY >> lod;
			if (segment.value.isAir()) {
				Segment<BlockState> existing = newList.getOverlappingSegment(minY);
				if (existing != null && !existing.value.isAir()) {
					minY = Math.max(minY, existing.maxY + 1);
				}
			}
			//ensure liquids can't overwrite normal blocks.
			else if (segment.value.getBlock() instanceof FluidBlock) {
				Segment<BlockState> existing = newList.getOverlappingSegment(minY);
				if (existing != null && !existing.value.isAir() && existing.value.getFluidState().isEmpty()) {
					minY = Math.min(Math.max(minY, existing.maxY + 1), maxY);
				}
			}
			newList.addSegment(minY, maxY, segment.value);
		}
		return newList;
	}

	public static boolean quickCheckRender(BlockState self, BlockState other) {
		VoxelShape shape = BlockStateVersions.getCullingShape(other, EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
		if (shape == VoxelShapes.fullCube()) return false;
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
					LitSegment centerSegment = center.getLit(centerIndex);
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
									Segment<BlockState> adjacentSegment = adjacent.getOverlappingSegment(y);
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
											provider.getBuffer(RenderLayers.getFluidLayer(fluidState)),
											centerSegment.value,
											fluidState
										);
									}
									else {
										blockRenderManager.renderFluid(
											pos,
											columnBlockView,
											provider.getBuffer(RenderLayers.getFluidLayer(fluidState)),
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

	public static record LodRequest(
		LodSystem system,
		LodQuadTree owner,
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

		public void apply(MeshUploader uploader) {
			LodQuadTree owner = this.request.owner;
			try {
				if (owner.passes == null && owner.isQueued()) {
					if (this.success) {
						owner.passes = uploader.upload(this.request.provider);
					}
					else {
						owner.setQueued(false);
					}
				}
			}
			catch (Throwable throwable) {
				if (owner.passes == null && owner.isQueued()) {
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
			return this.columns[z * RENDER_WIDTH + x];
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
			return switch (type) {
				case BLOCK -> {
					yield 0;
				}
				case SKY -> {
					int x = pos.getX() + SINGLE_RENDER_PADDING;
					if (x < 0 || x >= RENDER_WIDTH) yield 15;
					int z = pos.getZ() + SINGLE_RENDER_PADDING;
					if (z < 0 || z >= RENDER_WIDTH) yield 15;
					Segment<BlockState> segment = this.lists[z * RENDER_WIDTH + x].getOverlappingSegment(pos.getY());
					yield segment instanceof LitSegment lit ? lit.getLightLevel(pos.getY(), this.lod - LodQuadTree.MIN_LEVEL) : 15;
				}
			};
		}
	}
}