package builderb0y.bigglobe.util.coordinators;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class WorldCoordinator extends ScratchPosCoordinator {

	public final WorldGenLevel world;
	public final int setBlockFlags;

	public WorldCoordinator(WorldGenLevel world, int setBlockFlags) {
		this.world = world;
		this.setBlockFlags = setBlockFlags;
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
	public void getBlockState(int x, int y, int z, CoordinateConsumer<BlockState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getBlockState(this.scratchPos));
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateConsumer<FluidState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getFluidState(this.scratchPos));
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateConsumer<BlockEntity> action) {
		BlockEntity blockEntity = this.world.getBlockEntity(this.scratchPos.set(x, y, z));
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> blockEntityType, CoordinateConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateConsumer<ChunkAccess> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getChunk(x >> 4, z >> 4));
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		this.world.setBlock(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (supplier == null) return;
		BlockState state = supplier.get(this.scratchPos.set(x, y, z));
		if (state == null) return;
		this.world.setBlock(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) {
		if (state == null) return;
		this.world.setBlock(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos, blockEntityClass);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {
		if (state == null) return;
		this.world.setBlock(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos, blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		BlockState oldState = this.world.getBlockState(this.scratchPos.set(x, y, z));
		BlockState newState = mapper.apply(this.scratchPos, oldState);
		if (oldState != newState && newState != null) {
			this.world.setBlock(this.scratchPos.set(x, y, z), newState, this.setBlockFlags);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<AABB> boxSupplier, CoordinateConsumer<List<E>> entityAction) {
		AABB box = boxSupplier.get(this.scratchPos.set(x, y, z));
		entityAction.accept(this.scratchPos.set(x, y, z), this.world.getEntitiesOfClass(entityType, box));
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerLevel, Entity> supplier) {
		Entity entity = supplier.apply(this.scratchPos.set(x, y, z), this.world.getLevel());
		if (entity != null) this.world.addFreshEntityWithPassengers(entity);
	}

	@Override
	public int hashCode() {
		return this.world.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof WorldCoordinator that &&
			this.world.equals(that.world)
		);
	}

	@Override
	public String toString() {
		return "WorldCoordinator: { " + this.world + " }";
	}
}