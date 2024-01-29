package builderb0y.bigglobe.util;

import java.util.function.Function;
import java.util.random.RandomGenerator;

import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.columns.AbstractChunkOfColumns.ColumnFactory;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.BlockQueueStructureWorldAccess;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.MojangPermuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.WorldVersions;

/**
in order for {@link ScriptedStructure} to support raw generation,
{@link WorldWrapper} needs to be able to do things with worlds and chunks alike.
that's where this interface comes into play: it extracts out the common logic
between worlds and chunks, and allows {@link WorldWrapper} to operate on both.
*/
public interface WorldOrChunk extends BlockView {

	public abstract boolean isLive();

	public abstract void setBlockState(BlockPos pos, BlockState state);

	public abstract boolean placeBlockState(BlockPos pos, BlockState state);

	public abstract boolean canPlace(BlockPos pos, BlockState state);

	public abstract void scheduleFluidTick(BlockPos pos, FluidState state);

	public abstract long getSeed();

	public abstract Chunk getChunk(int x, int z, ChunkStatus status, boolean create);

	public abstract boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> feature, Random random);

	public abstract void spawnEntity(Function<ServerWorld, Entity> entitySupplier);

	public abstract Coordinator coordinator();

	public abstract void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlacementData data, RandomGenerator random);

	public static class WorldDelegator implements WorldOrChunk {

		public final StructureWorldAccess world;

		public WorldDelegator(StructureWorldAccess world) {
			this.world = world;
		}

		@Override
		public boolean isLive() {
			StructureWorldAccess world = this.world;
			while (world instanceof BlockQueueStructureWorldAccess queue) {
				world = queue.world;
			}
			return world instanceof World;
		}

		@Override
		public int getBottomY() {
			return this.world.getBottomY();
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
			WorldUtil.setBlockState(this.world, pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}

		@Override
		public boolean placeBlockState(BlockPos pos, BlockState state) {
			return SingleBlockFeature.place(this.world, pos, state, SingleBlockFeature.IS_REPLACEABLE);
		}

		@Override
		public boolean canPlace(BlockPos pos, BlockState state) {
			return state.canPlaceAt(this.world, pos);
		}

		@Override
		public void scheduleFluidTick(BlockPos pos, FluidState state) {
			WorldVersions.scheduleFluidTick(this.world, pos, state.getFluid(), state.getFluid().getTickRate(this.world));
		}

		@Override
		public long getSeed() {
			return this.world.getSeed();
		}

		@Override
		public Chunk getChunk(int x, int z, ChunkStatus status, boolean create) {
			return this.world.getChunk(x, z, status, create);
		}

		@Override
		public boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> feature, Random random) {
			return feature.generate(
				this.world,
				((ServerChunkManager)(this.world.getChunkManager())).getChunkGenerator(),
				random,
				pos
			);
		}

		@Override
		public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlacementData data, RandomGenerator random) {
			template.place(
				this.world,
				new BlockPos(x, y, z),
				data.getPosition(),
				data,
				MojangPermuter.from(random),
				Block.NOTIFY_LISTENERS | Block.FORCE_STATE
			);
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return this.world.getBlockEntity(pos);
		}

		@Override
		public void spawnEntity(Function<ServerWorld, Entity> entitySupplier) {
			Entity entity = entitySupplier.apply(this.world.toServerWorld());
			if (entity != null) this.world.spawnEntityAndPassengers(entity);
		}

		@Override
		public Coordinator coordinator() {
			return Coordinator.forWorld(this.world, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}
	}

	public static class ChunkDelegator implements WorldOrChunk {

		public final Chunk chunk;
		public final long seed;

		public ChunkDelegator(Chunk chunk, long seed) {
			this.chunk = chunk;
			this.seed = seed;
		}

		@Override
		public boolean isLive() {
			return this.chunk instanceof WorldChunk;
		}

		@Override
		public int getBottomY() {
			return this.chunk.getBottomY();
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
			this.chunk.setBlockState(pos, state, false);
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
		public Chunk getChunk(int x, int z, ChunkStatus status, boolean create) {
			if (this.chunk.getPos().x == x && this.chunk.getPos().z == z && this.chunk.getStatus().isAtLeast(status)) {
				return this.chunk;
			}
			else if (create) {
				throw new UnsupportedOperationException("Can't create chunk.");
			}
			else {
				return null;
			}
		}

		@Override
		public boolean placeFeature(BlockPos pos, ConfiguredFeature<?, ?> feature, Random random) {
			//todo: add RawFeature interface to whitelist features that can be placed during raw generation.
			throw new UnsupportedOperationException("Can't place features during raw generation.");
		}

		@Override
		public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlacementData data, RandomGenerator random) {
			throw new UnsupportedOperationException("Can't place structure templates during raw generation.");
		}

		@Override
		public BlockEntity getBlockEntity(BlockPos pos) {
			return this.chunk.getBlockEntity(pos);
		}

		@Override
		public void spawnEntity(Function<ServerWorld, Entity> entitySupplier) {
			throw new UnsupportedOperationException("Can't spawn entities during raw generation.");
		}

		@Override
		public Coordinator coordinator() {
			return Coordinator.forChunk(this.chunk);
		}
	}
}