package builderb0y.bigglobe.util;

import java.util.function.Function;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import builderb0y.bigglobe.features.RawFeature;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.bigglobe.util.coordinators.Coordinator;

/**
in order for {@link ScriptedStructure} to support raw generation,
{@link WorldWrapper} needs to be able to do things with worlds and chunks alike.
that's where this interface comes into play: it extracts out the common logic
between worlds and chunks, and allows {@link WorldWrapper} to operate on both.
*/
public interface WorldOrChunk extends BlockGetter {

	public abstract void setBlockState(BlockPos pos, BlockState state);

	public abstract boolean placeBlockState(BlockPos pos, BlockState state);

	public abstract void updateBlockState(BlockPos pos);

	public abstract boolean canPlace(BlockPos pos, BlockState state);

	public abstract void scheduleFluidTick(BlockPos pos, FluidState state);

	public abstract long getSeed();

	public abstract boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> feature, RandomSource random);

	public abstract void spawnEntity(Function<ServerLevel, Entity> entitySupplier);

	public abstract Coordinator coordinator();

	public abstract void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlaceSettings data, RandomGenerator random);

	public static class WorldDelegator implements WorldOrChunk {

		public final WorldGenLevel world;

		public WorldDelegator(WorldGenLevel world) {
			this.world = world;
		}

		@Override
		public int getMinY() {
			return this.world.getMinY();
		}

		@Override
		public int getHeight() {
			return this.world.getHeight();
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			return this.world.getBlockState(pos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return this.world.getFluidState(pos);
		}

		@Override
		public void setBlockState(BlockPos pos, BlockState state) {
			if (state != null) {
				WorldUtil.setBlockState(this.world, pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}

		@Override
		public void updateBlockState(BlockPos pos) {
			BlockState oldState = this.getBlockState(pos);
			BlockState newState = Block.updateFromNeighbourShapes(oldState, this.world, pos);
			if (oldState != newState) this.setBlockState(pos, newState);
		}

		@Override
		public boolean placeBlockState(BlockPos pos, BlockState state) {
			return SingleBlockFeature.place(this.world, pos, state, SingleBlockFeature.IS_REPLACEABLE);
		}

		@Override
		public boolean canPlace(BlockPos pos, BlockState state) {
			return state.canSurvive(this.world, pos);
		}

		@Override
		public void scheduleFluidTick(BlockPos pos, FluidState state) {
			this.world.scheduleTick(pos, state.getType(), state.getType().getTickDelay(this.world));
		}

		@Override
		public long getSeed() {
			return this.world.getSeed();
		}

		@Override
		public boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> feature, RandomSource random) {
			return feature.place(
				this.world,
				((ServerChunkCache)(this.world.getChunkSource())).getGenerator(),
				random,
				pos
			);
		}

		@Override
		public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlaceSettings data, RandomGenerator random) {
			template.placeInWorld(
				this.world,
				new BlockPos(x, y, z),
				data.getRotationPivot(),
				data,
				MojangPermuter.from(random),
				Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
			);
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return this.world.getBlockEntity(pos);
		}

		@Override
		public void spawnEntity(Function<ServerLevel, Entity> entitySupplier) {
			Entity entity = entitySupplier.apply(this.world.getLevel());
			if (entity != null) this.world.addFreshEntityWithPassengers(entity);
		}

		@Override
		public Coordinator coordinator() {
			return Coordinator.forWorld(this.world, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
	}

	public static class ChunkDelegator implements WorldOrChunk {

		public final ChunkAccess chunk;
		public final long seed;
		public WorldWrapper worldWrapper;

		public ChunkDelegator(ChunkAccess chunk, long seed) {
			this.chunk = chunk;
			this.seed = seed;
		}

		@Override
		public int getMinY() {
			return this.chunk.getMinY();
		}

		@Override
		public int getHeight() {
			return this.chunk.getHeight();
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			return this.chunk.getBlockState(pos);
		}

		@Override
		public FluidState getFluidState(BlockPos pos) {
			return this.chunk.getFluidState(pos);
		}

		@Override
		public void setBlockState(BlockPos pos, BlockState state) {
			if (state != null) {
				this.chunk.setBlockState(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				if (state.hasBlockEntity()) {
					BlockEntity blockEntity = ((EntityBlock)(state.getBlock())).newBlockEntity(pos, state);
					if (blockEntity != null) this.chunk.setBlockEntity(blockEntity);
				}
			}
		}

		@Override
		public void updateBlockState(BlockPos pos) {
			this.chunk.markPosForPostprocessing(pos);
		}

		@Override
		public boolean placeBlockState(BlockPos pos, BlockState state) {
			return SingleBlockFeature.placeEarly(this.chunk, pos, state, SingleBlockFeature.IS_REPLACEABLE);
		}

		@Override
		public boolean canPlace(BlockPos pos, BlockState state) {
			return true;
		}

		@Override
		public void scheduleFluidTick(BlockPos pos, FluidState state) {
			//no-op.
		}

		@Override
		public long getSeed() {
			return this.seed;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> configuredFeature, RandomSource random) {
			if (configuredFeature.feature() instanceof RawFeature rawFeature) {
				return rawFeature.generate(this.worldWrapper, configuredFeature.config(), pos);
			}
			else {
				throw new UnsupportedOperationException("The provided feature cannot generate during raw generation.");
			}
		}

		@Override
		public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlaceSettings data, RandomGenerator random) {
			throw new UnsupportedOperationException("Can't place structure templates during raw generation.");
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return this.chunk.getBlockEntity(pos);
		}

		@Override
		public void spawnEntity(Function<ServerLevel, Entity> entitySupplier) {
			throw new UnsupportedOperationException("Can't spawn entities during raw generation.");
		}

		@Override
		public Coordinator coordinator() {
			return Coordinator.forChunk(this.chunk);
		}
	}
}