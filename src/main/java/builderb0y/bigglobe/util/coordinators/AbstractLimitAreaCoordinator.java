package builderb0y.bigglobe.util.coordinators;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.registry.BetterRegistryEntry;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public abstract class AbstractLimitAreaCoordinator implements Coordinator {

	public final Coordinator delegate;

	public AbstractLimitAreaCoordinator(Coordinator delegate) {
		this.delegate = delegate;
	}

	public abstract boolean test(int x, int y, int z);

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		if (this.test(x, y, z)) {
			this.delegate.getWorld(x, y, z, action);
		}
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		if (this.test(x, y, z)) {
			this.delegate.getCoordinates(x, y, z, action);
		}
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		if (this.test(x, y, z)) {
			this.delegate.getBlockState(x, y, z, action);
		}
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		if (this.test(x, y, z)) {
			this.delegate.getFluidState(x, y, z, action);
		}
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		if (this.test(x, y, z)) {
			this.delegate.getBlockEntity(x, y, z, action);
		}
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		if (this.test(x, y, z)) {
			this.delegate.getBlockEntity(x, y, z, tileEntityType, action);
		}
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		if (this.test(x, y, z)) {
			this.delegate.getBlockEntity(x, y, z, tileEntityType, action);
		}
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<BetterRegistryEntry<Biome>> action) {
		if (this.test(x, y, z)) {
			this.delegate.getBiome(x, y, z, action);
		}
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		if (this.test(x, y, z)) {
			this.delegate.getChunk(x, y, z, action);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state != null && this.test(x, y, z)) {
			this.delegate.setBlockState(x, y, z, state);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (this.test(x, y, z)) {
			this.delegate.setBlockState(x, y, z, supplier);
		}
	}

	@Override
	public void setBlockStateRelative(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (this.test(x, y, z)) {
			this.delegate.setBlockStateRelative(x, y, z, supplier);
		}
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state != null && this.test(x, y, z)) {
			this.delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
		}
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state != null && this.test(x, y, z)) {
			this.delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
		}
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		if (this.test(x, y, z)) {
			this.delegate.modifyBlockState(x, y, z, mapper);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		if (this.test(x, y, z)) {
			this.delegate.getEntities(x, y, z, entityType, boxSupplier, entityAction);
		}
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		if (this.test(x, y, z)) {
			this.delegate.addEntity(x, y, z, supplier);
		}
	}

	public static class LimitArea extends AbstractLimitAreaCoordinator {

		public final CoordinatePredicate predicate;
		public final BlockPos.Mutable scratchPos;

		public LimitArea(Coordinator delegate, CoordinatePredicate predicate) {
			super(delegate);
			this.predicate = predicate;
			this.scratchPos = new BlockPos.Mutable();
		}

		@Override
		public boolean test(int x, int y, int z) {
			return this.predicate.test(this.scratchPos.set(x, y, z));
		}

		@Override
		public Coordinator limitArea(CoordinatePredicate predicate) {
			return this.delegate.limitArea(this.predicate.and(predicate));
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ this.predicate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LimitArea)) return false;
			LimitArea that = (LimitArea)(obj);
			return this.delegate.equals(that.delegate) && this.predicate.equals(that.predicate);
		}

		@Override
		public String toString() {
			return this.delegate + " limited by " + this.predicate;
		}
	}

	public static class InBox extends AbstractLimitAreaCoordinator {

		public final int minX, minY, minZ, maxX, maxY, maxZ;

		public InBox(Coordinator delegate, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			super(delegate);
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.maxX = maxX;
			this.maxY = maxY;
			this.maxZ = maxZ;
		}

		public InBox(Coordinator delegate, BlockBox box) {
			this(delegate, box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
		}

		@Override
		public boolean test(int x, int y, int z) {
			return (
				x >= this.minX && x <= this.maxX &&
				z >= this.minZ && z <= this.maxZ &&
				y >= this.minY && y <= this.maxY
			);
		}

		@Override
		public Coordinator inBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
			return this.delegate.inBox(
				Math.max(this.minX, minX),
				Math.max(this.minY, minY),
				Math.max(this.minZ, minZ),
				Math.min(this.maxX, maxX),
				Math.min(this.maxY, maxY),
				Math.min(this.maxZ, maxZ)
			);
		}

		@Override
		public int hashCode() {
			int hash = this.delegate.hashCode();
			hash = hash * 31 + this.minX;
			hash = hash * 31 + this.minY;
			hash = hash * 31 + this.minZ;
			hash = hash * 31 + this.maxX;
			hash = hash * 31 + this.maxY;
			hash = hash * 31 + this.maxZ;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof InBox)) return false;
			InBox that = (InBox)(obj);
			return (
				this.delegate.equals(that.delegate) &&
				this.minX == that.minX &&
				this.minY == that.minY &&
				this.minZ == that.minZ &&
				this.maxX == that.maxX &&
				this.maxY == that.maxY &&
				this.maxZ == that.maxZ
			);
		}

		@Override
		public String toString() {
			return this.delegate + " in box (" + this.minX + ", " + this.minY + ", " + this.minZ + ") to (" + this.maxX + ", " + this.maxY + ", " + this.maxZ + ')';
		}
	}

	public static class LazyInBox extends AbstractLimitAreaCoordinator {

		public final BlockBox box;

		public LazyInBox(Coordinator delegate, BlockBox box) {
			super(delegate);
			this.box = box;
		}

		@Override
		public boolean test(int x, int y, int z) {
			return (
				x >= this.box.getMinX() && x <= this.box.getMaxX() &&
				z >= this.box.getMinZ() && z <= this.box.getMaxZ() &&
				y >= this.box.getMinY() && y <= this.box.getMaxY()
			);
		}

		@Override
		public int hashCode() {
			return this.delegate.hashCode() ^ System.identityHashCode(this.box);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof LazyInBox)) return false;
			LazyInBox that = (LazyInBox)(obj);
			return this.delegate.equals(that.delegate) && this.box == that.box;
		}

		@Override
		public String toString() {
			return this.delegate + " in lazy box currently at (" + this.box.getMinX() + ", " + this.box.getMinY() + ", " + this.box.getMinZ() + ") to (" + this.box.getMaxX() + ", " + this.box.getMaxY() + ", " + this.box.getMaxZ() + ')';
		}
	}
}