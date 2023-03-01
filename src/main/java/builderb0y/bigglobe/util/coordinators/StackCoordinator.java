package builderb0y.bigglobe.util.coordinators;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class StackCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;
	public final int dx, dy, dz, count;

	public StackCoordinator(Coordinator delegate, int dx, int dy, int dz, int count) {
		assert count > 1;
		assert dx != 0 || dy != 0 || dz != 0;
		this.delegate = delegate;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.count = count;
	}

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		this.delegate.getWorld(x, y, z, action);
		this.delegate.getWorld(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getWorld(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		this.delegate.getCoordinates(x, y, z, action);
		this.delegate.getCoordinates(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getCoordinates(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		this.delegate.getBlockState(x, y, z, action);
		this.delegate.getBlockState(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getBlockState(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		this.delegate.getFluidState(x, y, z, action);
		this.delegate.getFluidState(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getFluidState(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		this.delegate.getBlockEntity(x, y, z, action);
		this.delegate.getBlockEntity(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getBlockEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		this.delegate.getBlockEntity(x, y, z, tileEntityType, action);
		this.delegate.getBlockEntity(x + this.dx, y + this.dy, z + this.dz, tileEntityType, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getBlockEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, tileEntityType, action);
		}
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		this.delegate.getBlockEntity(x, y, z, tileEntityType, action);
		this.delegate.getBlockEntity(x + this.dx, y + this.dy, z + this.dz, tileEntityType, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getBlockEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, tileEntityType, action);
		}
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		this.delegate.getBiome(x, y, z, action);
		this.delegate.getBiome(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getBiome(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		this.delegate.getChunk(x, y, z, action);
		this.delegate.getChunk(x + this.dx, y + this.dy, z + this.dz, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getChunk(x + this.dx * i, y + this.dy * i, z + this.dz * i, action);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		this.delegate.setBlockState(x, y, z, state);
		this.delegate.setBlockState(x + this.dx, y + this.dy, z + this.dz, state);
		for (int i = 2; i < this.count; i++) {
			this.delegate.setBlockState(x + this.dx * i, y + this.dy * i, z + this.dz * i, state);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		this.delegate.setBlockState(x, y, z, supplier);
		this.delegate.setBlockState(x + this.dx, y + this.dy, z + this.dz, supplier);
		for (int i = 2; i < this.count; i++) {
			this.delegate.setBlockState(x + this.dx * i, y + this.dy * i, z + this.dz * i, supplier);
		}
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
		this.delegate.setBlockStateAndBlockEntity(x + this.dx, y + this.dy, z + this.dz, state, blockEntityClass, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.setBlockStateAndBlockEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, state, blockEntityClass, action);
		}
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
		this.delegate.setBlockStateAndBlockEntity(x + this.dx, y + this.dy, z + this.dz, state, blockEntityType, action);
		for (int i = 2; i < this.count; i++) {
			this.delegate.setBlockStateAndBlockEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, state, blockEntityType, action);
		}
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		this.delegate.modifyBlockState(x, y, z, mapper);
		this.delegate.modifyBlockState(x + this.dx, y + this.dy, z + this.dz, mapper);
		for (int i = 2; i < this.count; i++) {
			this.delegate.modifyBlockState(x + this.dx * i, y + this.dy * i, z + this.dz * i, mapper);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		this.delegate.getEntities(x, y, z, entityType, boxSupplier, entityAction);
		this.delegate.getEntities(x + this.dx, y + this.dy, z + this.dz, entityType, boxSupplier, entityAction);
		for (int i = 2; i < this.count; i++) {
			this.delegate.getEntities(x + this.dx * i, y + this.dy * i, z + this.dz * i, entityType, boxSupplier, entityAction);
		}
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		this.delegate.addEntity(x, y, z, supplier);
		this.delegate.addEntity(x + this.dx, y + this.dy, z + this.dz, supplier);
		for (int i = 2; i < this.count; i++) {
			this.delegate.addEntity(x + this.dx * i, y + this.dy * i, z + this.dz * i, supplier);
		}
	}

	@Override
	public int hashCode() {
		int hash = this.delegate.hashCode();
		hash = hash * 31 + this.dx;
		hash = hash * 31 + this.dy;
		hash = hash * 31 + this.dz;
		hash = hash * 31 + this.count;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof StackCoordinator)) return false;
		StackCoordinator that = (StackCoordinator)(obj);
		return (
			this.delegate.equals(that.delegate) &&
			this.dx == that.dx &&
			this.dy == that.dy &&
			this.dz == that.dz &&
			this.count == that.count
		);
	}

	@Override
	public String toString() {
		return this.delegate.toString() + " stacked " + this.count + "x (" + this.dx + ", " + this.dy + ", " + this.dz + ')';
	}
}