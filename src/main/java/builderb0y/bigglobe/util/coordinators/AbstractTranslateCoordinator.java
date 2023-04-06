package builderb0y.bigglobe.util.coordinators;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.registry.BetterRegistryEntry;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public abstract class AbstractTranslateCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;

	public AbstractTranslateCoordinator(Coordinator delegate) {
		this.delegate = delegate;
	}

	public abstract int offsetX();

	public abstract int offsetY();

	public abstract int offsetZ();

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		this.delegate.getWorld(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		this.delegate.getCoordinates(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		this.delegate.getBlockState(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		this.delegate.getFluidState(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		this.delegate.getBlockEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		this.delegate.getBlockEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), tileEntityType, action);
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		this.delegate.getBlockEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), tileEntityType, action);
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<BetterRegistryEntry<Biome>> action) {
		this.delegate.getBiome(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		this.delegate.getChunk(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), action);
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		this.delegate.setBlockState(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), state);
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		this.delegate.setBlockState(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), supplier);
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.delegate.setBlockStateAndBlockEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), state, blockEntityClass, action);
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.delegate.setBlockStateAndBlockEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), state, blockEntityType, action);
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		this.delegate.modifyBlockState(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), mapper);
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		this.delegate.getEntities(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), entityType, boxSupplier, entityAction);
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		this.delegate.addEntity(x + this.offsetX(), y + this.offsetY(), z + this.offsetZ(), supplier);
	}

	public static class TranslateCoordinator extends AbstractTranslateCoordinator {

		public final int offsetX, offsetY, offsetZ;

		public TranslateCoordinator(Coordinator delegate, int offsetX, int offsetY, int offsetZ) {
			super(delegate);
			assert offsetX != 0 || offsetY != 0 || offsetZ != 0;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
		}

		@Override public int offsetX() { return this.offsetX; }
		@Override public int offsetY() { return this.offsetY; }
		@Override public int offsetZ() { return this.offsetZ; }

		@Override
		public Coordinator translate(int offsetX, int offsetY, int offsetZ) {
			return this.delegate.translate(offsetX + this.offsetX(), offsetY + this.offsetY(), offsetZ + this.offsetZ());
		}

		@Override
		public int hashCode() {
			int hash = this.delegate.hashCode();
			hash = hash * 31 + this.offsetX;
			hash = hash * 31 + this.offsetY;
			hash = hash * 31 + this.offsetZ;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof TranslateCoordinator)) return false;
			TranslateCoordinator that = (TranslateCoordinator)(obj);
			return (
				this.delegate.equals(that.delegate) &&
				this.offsetX == that.offsetX &&
				this.offsetY == that.offsetY &&
				this.offsetZ == that.offsetZ
			);
		}

		@Override
		public String toString() {
			return this.delegate + " translated by (" + this.offsetX + ", " + this.offsetY + ", " + this.offsetZ + ')';
		}
	}

	public static class LazyTranslateCoordinator extends AbstractTranslateCoordinator {

		public final Vec3i offset;

		public LazyTranslateCoordinator(Coordinator delegate, Vec3i offset) {
			super(delegate);
			this.offset = offset;
		}

		@Override public int offsetX() { return this.offset.getX(); }
		@Override public int offsetY() { return this.offset.getY(); }
		@Override public int offsetZ() { return this.offset.getZ(); }

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ System.identityHashCode(this.offset);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LazyTranslateCoordinator)) return false;
			LazyTranslateCoordinator that = (LazyTranslateCoordinator)(obj);
			return this.delegate.equals(that.delegate) && this.offset == that.offset;
		}

		@Override
		public String toString() {
			return this.delegate + " lazily translated by currently (" + this.offset.getX() + ", " + this.offset.getY() + ", " + this.offset.getZ() + ')';
		}
	}
}