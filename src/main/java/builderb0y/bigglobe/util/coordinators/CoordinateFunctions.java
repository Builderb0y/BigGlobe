package builderb0y.bigglobe.util.coordinators;

import java.util.List;
import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.Chunk;

public class CoordinateFunctions {

	@FunctionalInterface
	public static interface CoordinateRunnable {

		public abstract void accept(BlockPos.Mutable pos);

		public default CoordinateRunnable then(CoordinateRunnable other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos) -> {
				this.accept(pos);
				other.accept(pos);
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinateConsumer<T> {

		public abstract void accept(BlockPos.Mutable pos, T arg);

		public default CoordinateConsumer<T> then(CoordinateConsumer<? super T> other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos, T arg) -> {
				this.accept(pos, arg);
				other.accept(pos, arg);
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinateSupplier<R> {

		public abstract R get(BlockPos.Mutable pos);
	}

	@FunctionalInterface
	public static interface CoordinateFunction<T, R> {

		public abstract R apply(BlockPos.Mutable pos, T arg);
	}

	@FunctionalInterface
	public static interface CoordinateUnaryOperator<T> extends CoordinateFunction<T, T> {}

	@FunctionalInterface
	public static interface CoordinateBooleanSupplier {

		public abstract boolean test(BlockPos.Mutable pos);

		public default CoordinateBooleanSupplier or(CoordinateBooleanSupplier other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos) || other.test(pos.set(x, y, z));
			};
		}

		public default CoordinateBooleanSupplier and(CoordinateBooleanSupplier other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos) && other.test(pos.set(x, y, z));
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinatePredicate<T> {

		public abstract boolean test(BlockPos.Mutable pos, T arg);

		public default CoordinatePredicate<T> or(CoordinatePredicate<? super T> other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos, T arg) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos, arg) || other.test(pos.set(x, y, z), arg);
			};
		}

		public default CoordinatePredicate<T> and(CoordinatePredicate<? super T> other) {
			Objects.requireNonNull(other);
			return (BlockPos.Mutable pos, T arg) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos, arg) && other.test(pos.set(x, y, z), arg);
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinatorRunnable {

		public abstract void run(Coordinator coordinator, int x, int y, int z);
	}

	@FunctionalInterface
	public static interface CoordinatorConsumer<T> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, T arg);

		//////////////////////////////// internal helper methods ////////////////////////////////

		public static CoordinatorConsumer<CoordinateRunnable> getCoordinates() {
			return Coordinator::getCoordinates;
		}

		public static CoordinatorConsumer<CoordinateConsumer<BlockState>> getBlockState() {
			return Coordinator::getBlockState;
		}

		public static CoordinatorConsumer<CoordinateConsumer<FluidState>> getFluidState() {
			return Coordinator::getFluidState;
		}

		public static CoordinatorConsumer<CoordinateConsumer<BlockEntity>> getBlockEntity() {
			return Coordinator::getBlockEntity;
		}

		public static CoordinatorConsumer<CoordinateConsumer<Chunk>> getChunk() {
			return Coordinator::getChunk;
		}

		public static CoordinatorConsumer<BlockState> setBlockState() {
			return Coordinator::setBlockState;
		}

		public static CoordinatorConsumer<CoordinateSupplier<BlockState>> setBlockState_supplier() {
			return Coordinator::setBlockState;
		}

		public static CoordinatorConsumer<CoordinateUnaryOperator<BlockState>> modifyBlockState() {
			return Coordinator::modifyBlockState;
		}

		public static CoordinatorConsumer<CoordinateFunction<ServerWorld, Entity>> addEntity() {
			return Coordinator::addEntity;
		}
	}

	@FunctionalInterface
	public static interface CoordinatorBiConsumer<A, B> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, A arg1, B arg2);

		public static <B> CoordinatorBiConsumer<Class<B>, CoordinateConsumer<B>> getBlockEntityByClass() {
			return Coordinator::getBlockEntity;
		}

		public static <B extends BlockEntity> CoordinatorBiConsumer<BlockEntityType<B>, CoordinateConsumer<B>> getBlockEntityByType() {
			return Coordinator::getBlockEntity;
		}
	}

	@FunctionalInterface
	public static interface CoordinatorTriConsumer<A, B, C> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, A arg1, B arg2, C arg3);

		public static <B> CoordinatorTriConsumer<BlockState, Class<B>, CoordinateConsumer<B>> setBlockStateAndBlockEntityByClass() {
			return Coordinator::setBlockStateAndBlockEntity;
		}

		public static <B extends BlockEntity> CoordinatorTriConsumer<BlockState, BlockEntityType<B>, CoordinateConsumer<B>> setBlockStateAndBlockEntityByType() {
			return Coordinator::setBlockStateAndBlockEntity;
		}

		public static <E extends Entity> CoordinatorTriConsumer<Class<E>, CoordinateSupplier<Box>, CoordinateConsumer<List<E>>> getEntities() {
			return Coordinator::getEntities;
		}
	}

	@FunctionalInterface
	public static interface LineRunnable {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index);
	}

	@FunctionalInterface
	public static interface LineConsumer<T> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index, T arg);

		//////////////////////////////// internal helper methods ////////////////////////////////

		public static LineConsumer<CoordinateRunnable> getCoordinates() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateRunnable action) -> coordinator.getCoordinates(x, y, z, action);
		}

		public static LineConsumer<CoordinateConsumer<BlockState>> getBlockState() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateConsumer<BlockState> action) -> coordinator.getBlockState(x, y, z, action);
		}

		public static LineConsumer<CoordinateConsumer<FluidState>> getFluidState() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateConsumer<FluidState> action) -> coordinator.getFluidState(x, y, z, action);
		}

		public static LineConsumer<CoordinateConsumer<BlockEntity>> getBlockEntity() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateConsumer<BlockEntity> action) -> coordinator.getBlockEntity(x, y, z, action);
		}

		public static LineConsumer<CoordinateConsumer<Chunk>> getChunk() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateConsumer<Chunk> action) -> coordinator.getChunk(x, y, z, action);
		}

		public static LineConsumer<BlockState> setBlockState() {
			return (Coordinator coordinator, int x, int y, int z, int index, BlockState state) -> coordinator.setBlockState(x, y, z, state);
		}

		public static LineConsumer<CoordinateSupplier<BlockState>> setBlockState_supplier() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateSupplier<BlockState> supplier) -> coordinator.setBlockState(x, y, z, supplier);
		}

		public static LineConsumer<CoordinateUnaryOperator<BlockState>> modifyBlockState() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateUnaryOperator<BlockState> mapper) -> coordinator.modifyBlockState(x, y, z, mapper);
		}

		public static LineConsumer<CoordinateFunction<ServerWorld, Entity>> addEntity() {
			return (Coordinator coordinator, int x, int y, int z, int index, CoordinateFunction<ServerWorld, Entity> supplier) -> coordinator.addEntity(x, y, z, supplier);
		}
	}

	@FunctionalInterface
	public static interface LineBiConsumer<A, B> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index, A arg1, B arg2);

		public static <B> LineBiConsumer<Class<B>, CoordinateConsumer<B>> getBlockEntitiesByClass() {
			return (Coordinator coordinator, int x, int y, int z, int index, Class<B> blockEntityClass, CoordinateConsumer<B> action) -> coordinator.getBlockEntity(x, y, z, blockEntityClass, action);
		}

		public static <B extends BlockEntity> LineBiConsumer<BlockEntityType<B>, CoordinateConsumer<B>> getBlockEntitiesByType() {
			return (Coordinator coordinator, int x, int y, int z, int index, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) -> coordinator.getBlockEntity(x, y, z, blockEntityType, action);
		}
	}

	@FunctionalInterface
	public static interface LineTriConsumer<A, B, C> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index, A arg1, B arg2, C arg3);

		public static <B> LineTriConsumer<BlockState, Class<B>, CoordinateConsumer<B>> setBlockStateAndBlockEntityByClass() {
			return (Coordinator coordinator, int x, int y, int z, int index, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) -> coordinator.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
		}

		public static <B extends BlockEntity> LineTriConsumer<BlockState, BlockEntityType<B>, CoordinateConsumer<B>> setBlockStateAndBlockEntityByType() {
			return (Coordinator coordinator, int x, int y, int z, int index, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) -> coordinator.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
		}

		public static <E extends Entity> LineTriConsumer<Class<E>, CoordinateSupplier<Box>, CoordinateConsumer<List<E>>> getEntities() {
			return (Coordinator coordinator, int x, int y, int z, int index, Class<E> entityClass, CoordinateSupplier<Box> boxSupplier, CoordinateConsumer<List<E>> entityAction) -> coordinator.getEntities(x, y, z, entityClass, boxSupplier, entityAction);
		}
	}
}