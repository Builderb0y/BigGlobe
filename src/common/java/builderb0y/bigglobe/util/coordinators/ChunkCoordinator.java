package builderb0y.bigglobe.util.coordinators;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;
import builderb0y.bigglobe.versions.ChunkVersions;

public class ChunkCoordinator extends ScratchPosCoordinator {

	public final ChunkAccess chunk;

	public ChunkCoordinator(ChunkAccess chunk) {
		this.chunk = chunk;
	}

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		callback.run(this, x, y, z);
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		callback.run(this, x, y, z, arg);
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		callback.run(this, x, y, z, arg1, arg2);
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		callback.run(this, x, y, z, arg1, arg2, arg3);
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateRunnable action) {
		action.accept(this.scratchPos.set(x, y, z));
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateConsumer<BlockState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk.getBlockState(this.scratchPos));
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateConsumer<FluidState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk.getFluidState(this.scratchPos));
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateConsumer<BlockEntity> action) {
		BlockEntity blockEntity = this.chunk.getBlockEntity(this.scratchPos.set(x, y, z));
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> blockEntityType, CoordinateConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateConsumer<ChunkAccess> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk);
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		ChunkVersions.setBlockState(this.chunk, this.scratchPos.set(x, y, z), state, Block.UPDATE_CLIENTS);
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (supplier == null) return;
		BlockState state = supplier.get(this.scratchPos.set(x, y, z));
		if (state == null) return;
		ChunkVersions.setBlockState(this.chunk, this.scratchPos.set(x, y, z), state, Block.UPDATE_CLIENTS);
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) {
		if (state == null) return;
		ChunkVersions.setBlockState(this.chunk, this.scratchPos.set(x, y, z), state, Block.UPDATE_CLIENTS);
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityClass);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {
		if (state == null) return;
		ChunkVersions.setBlockState(this.chunk, this.scratchPos.set(x, y, z), state, Block.UPDATE_CLIENTS);
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		BlockState oldState = this.chunk.getBlockState(this.scratchPos.set(x, y, z));
		BlockState newState = mapper.apply(this.scratchPos, oldState);
		if (newState != oldState && newState != null) {
			ChunkVersions.setBlockState(this.chunk, this.scratchPos.set(x, y, z), newState, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<AABB> boxSupplier, CoordinateConsumer<List<E>> entityAction) {
		throw new UnsupportedOperationException("Chunks don't store entities.");
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerLevel, Entity> supplier) {
		throw new UnsupportedOperationException("No world object to create entities from.");
	}

	@Override
	public int hashCode() {
		return this.chunk.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof ChunkCoordinator that &&
			this.chunk.equals(that.chunk)
		);
	}

	@Override
	public String toString() {
		return "ChunkCoordinator: { " + this.chunk + " }";
	}
}