package builderb0y.bigglobe.lods;

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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
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
import builderb0y.bigglobe.lods.LodRenderer.MeshUploader;
import builderb0y.bigglobe.util.AsyncRunner;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.versions.DirectionVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

@Environment(EnvType.CLIENT)
public class LodGenerator implements SafeCloseable {

	public static final int
		SINGLE_PADDING   = 2,
		DOUBLE_PADDING   = SINGLE_PADDING << 1,
		RENDER_WIDTH     = 1 << LodQuadTree.MIN_LEVEL,
		RENDER_AREA      = RENDER_WIDTH * RENDER_WIDTH,
		GENERATION_WIDTH = RENDER_WIDTH + DOUBLE_PADDING,
		GENERATION_AREA  = GENERATION_WIDTH * GENERATION_WIDTH;

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
			ScriptedColumn[] columns = new ScriptedColumn[GENERATION_AREA];
			for (int index = 0; index < GENERATION_AREA; index++) {
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

	public static record ColumnResults(ScriptedColumn[] columns, BlockSegmentList[] lists) {}

	public void buildRegion(LodRequest request) {
		ColumnResults results = this.generateColumns(request);
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
				this.columns.add(results.columns);
				this.activeMeshers.decrementAndGet();
			}
		});
	}

	public @Nullable ColumnResults generateColumns(LodRequest request) {
		int lod  = request.owner.level;
		int blockLod = lod - LodQuadTree.MIN_LEVEL;
		int step = 1 << blockLod;
		int quadSize = step << 1;
		int minX = request.owner.minX() - quadSize;
		int maxX = request.owner.maxX() + quadSize;
		int minZ = request.owner.minZ() - quadSize;
		int maxZ = request.owner.maxZ() + quadSize;
		ScriptedColumn[] columns;
		try {
			columns = this.columns.take();
		}
		catch (InterruptedException ignored) {
			return null;
		}
		BlockSegmentList[] lists = new BlockSegmentList[GENERATION_AREA];
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
						int baseIndex = ((quadZ_ - minZ) >> blockLod) * GENERATION_WIDTH + ((quadX_ - minX) >> blockLod);
						ScriptedColumn
							column00 = columns[baseIndex],
							column01 = columns[baseIndex + 1],
							column10 = columns[baseIndex + GENERATION_WIDTH],
							column11 = columns[baseIndex + (GENERATION_WIDTH + 1)];
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
						lists[baseIndex] = list00;
						lists[baseIndex + 1] = list01;
						lists[baseIndex + GENERATION_WIDTH] = list10;
						lists[baseIndex + (GENERATION_WIDTH + 1)] = list11;
					});
				}
			}
		}
		return new ColumnResults(columns, lists);
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

	public void buildGeometry(
		LodRequest request,
		ColumnResults results,
		VertexConsumerProvider provider
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
			results.columns,
			this.generatorParams
		);
		BlockSegmentList[] adjacents = new BlockSegmentList[4];
		BlockPos.Mutable pos = new BlockPos.Mutable();
		MatrixStack matrixStack = new MatrixStack();
		for (pos.setZ(0); pos.getZ() < RENDER_WIDTH; pos.setZ(pos.getZ() + 1)) {
			for (pos.setX(0); pos.getX() < RENDER_WIDTH; pos.setX(pos.getX() + 1)) {
				int baseColumnIndex = (pos.getZ() + SINGLE_PADDING) * GENERATION_WIDTH + (pos.getX() + SINGLE_PADDING);
				BlockSegmentList center = lists[baseColumnIndex];
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_X)] = lists[baseColumnIndex + 1];
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_X)] = lists[baseColumnIndex - 1];
				adjacents[DirectionVersions.horizontal(Directions.POSITIVE_Z)] = lists[baseColumnIndex + GENERATION_WIDTH];
				adjacents[DirectionVersions.horizontal(Directions.NEGATIVE_Z)] = lists[baseColumnIndex - GENERATION_WIDTH];
				for (int centerIndex = 0, centerSize = center.size(); centerIndex < centerSize; centerIndex++) {
					LitSegment centerSegment = center.getLit(centerIndex);
					if (!centerSegment.value.isAir()) {
						for (pos.setY(centerSegment.minY); pos.getY() <= centerSegment.maxY;) {
							int y = pos.getY();
							int nextY;
							boolean shouldRender;
							if (y == centerSegment.minY && centerIndex - 1 >= 0 && center.get(centerIndex - 1).value.getCullingShape(#if MC_VERSION <= MC_1_21_1 EmptyBlockView.INSTANCE, BlockPos.ORIGIN #endif) != VoxelShapes.fullCube()) {
								shouldRender = true;
								nextY = y + 1;
							}
							else if (y == centerSegment.maxY && centerIndex + 1 < centerSize && center.get(centerIndex + 1).value.getCullingShape(#if MC_VERSION <= MC_1_21_1 EmptyBlockView.INSTANCE, BlockPos.ORIGIN #endif) != VoxelShapes.fullCube()) {
								shouldRender = true;
								nextY = y + 1;
							}
							else {
								shouldRender = false;
								int skipTo = centerSegment.maxY;
								for (Direction direction : Directions.HORIZONTAL) {
									BlockSegmentList adjacent = adjacents[DirectionVersions.horizontal(direction)];
									Segment<BlockState> adjacentSegment = adjacent.getOverlappingSegment(y);
									if (adjacentSegment == null || adjacentSegment.value.getCullingShape(#if MC_VERSION <= MC_1_21_1 EmptyBlockView.INSTANCE, BlockPos.ORIGIN #endif) != VoxelShapes.fullCube()) {
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
											provider.getBuffer(RenderLayer.getTranslucent()),
											centerSegment.value,
											fluidState
										);
									}
									else {
										blockRenderManager.renderFluid(
											pos,
											columnBlockView,
											provider.getBuffer(RenderLayer.getTranslucent()),
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
		VertexConsumerProvider provider
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
			int x = Objects.checkIndex(pos.getX() + SINGLE_PADDING, GENERATION_WIDTH);
			int z = Objects.checkIndex(pos.getZ() + SINGLE_PADDING, GENERATION_WIDTH);
			return this.columns[z * GENERATION_WIDTH + x];
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
			int x = pos.getX() + SINGLE_PADDING;
			if (x < 0 || x > GENERATION_WIDTH) return BlockStates.AIR;
			int z = pos.getZ() + SINGLE_PADDING;
			if (z < 0 || z > GENERATION_WIDTH) return BlockStates.AIR;
			BlockState state = this.lists[z * GENERATION_WIDTH + x].getBlockState(pos.getY());
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
					int x = pos.getX() + SINGLE_PADDING;
					if (x < 0 || x > GENERATION_WIDTH) yield 15;
					int z = pos.getZ() + SINGLE_PADDING;
					if (z < 0 || z > GENERATION_WIDTH) yield 15;
					Segment<BlockState> segment = this.lists[z * GENERATION_WIDTH + x].getOverlappingSegment(pos.getY());
					//& 15 is probably completely unnecessary,
					//because while the light level does start out at -1,
					//it also gets overwritten later.
					//nevertheless, I don't want to risk ever dealing with this in the future.
					yield segment instanceof LitSegment lit ? lit.lightLevel & 15 : 15;
				}
			};
		}
	}
}