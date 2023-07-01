package builderb0y.bigglobe.util.coordinators;

import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class CoordinateFunctions {

	@FunctionalInterface
	public static interface CoordinateConsumer {

		public abstract void accept(BlockPos.Mutable pos);

		public default CoordinateConsumer then(CoordinateConsumer other) {
			Objects.requireNonNull(other);
			return pos -> {
				this.accept(pos);
				other.accept(pos);
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinateBiConsumer<T> {

		public abstract void accept(BlockPos.Mutable pos, T arg);

		public default CoordinateBiConsumer<T> then(CoordinateBiConsumer<? super T> other) {
			Objects.requireNonNull(other);
			return (pos, arg) -> {
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
	public static interface CoordinatePredicate {

		public abstract boolean test(BlockPos.Mutable pos);

		public default CoordinatePredicate or(CoordinatePredicate other) {
			Objects.requireNonNull(other);
			return pos -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos) || other.test(pos.set(x, y, z));
			};
		}

		public default CoordinatePredicate and(CoordinatePredicate other) {
			Objects.requireNonNull(other);
			return pos -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos) && other.test(pos.set(x, y, z));
			};
		}
	}

	@FunctionalInterface
	public static interface CoordinateBiPredicate<T> {

		public abstract boolean test(BlockPos.Mutable pos, T arg);

		public default CoordinateBiPredicate<T> or(CoordinateBiPredicate<? super T> other) {
			Objects.requireNonNull(other);
			return (pos, arg) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos, arg) || other.test(pos.set(x, y, z), arg);
			};
		}

		public default CoordinateBiPredicate<T> and(CoordinateBiPredicate<? super T> other) {
			Objects.requireNonNull(other);
			return (pos, arg) -> {
				int x = pos.getX(), y = pos.getY(), z = pos.getZ();
				return this.test(pos, arg) && other.test(pos.set(x, y, z), arg);
			};
		}
	}

	@FunctionalInterface
	public static interface CuboidCallback {

		public abstract void run(Coordinator coordinator, int x, int y, int z);
	}

	@FunctionalInterface
	public static interface CuboidBiCallback<T> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, T arg);

		//////////////////////////////// internal helper methods ////////////////////////////////

		public static CuboidBiCallback<CoordinateConsumer> getCoordinates() {
			return Coordinator::getCoordinates;
		}

		public static CuboidBiCallback<CoordinateBiConsumer<BlockState>> getBlockState() {
			return Coordinator::getBlockState;
		}

		public static CuboidBiCallback<CoordinateBiConsumer<FluidState>> getFluidState() {
			return Coordinator::getFluidState;
		}

		public static CuboidBiCallback<CoordinateBiConsumer<BlockEntity>> getBlockEntity() {
			return Coordinator::getBlockEntity;
		}

		public static CuboidBiCallback<CoordinateBiConsumer<RegistryEntry<Biome>>> getBiome() {
			return Coordinator::getBiome;
		}

		public static CuboidBiCallback<CoordinateBiConsumer<Chunk>> getChunk() {
			return Coordinator::getChunk;
		}

		public static CuboidBiCallback<BlockState> setBlockState() {
			return Coordinator::setBlockState;
		}

		public static CuboidBiCallback<CoordinateSupplier<BlockState>> setBlockState_supplier() {
			return Coordinator::setBlockState;
		}

		public static CuboidBiCallback<CoordinateSupplier<BlockState>> setBlockStateRelative() {
			return Coordinator::setBlockStateRelative;
		}

		public static CuboidBiCallback<CoordinateUnaryOperator<BlockState>> modifyBlockState() {
			return Coordinator::modifyBlockState;
		}

		public static CuboidBiCallback<CoordinateFunction<ServerWorld, Entity>> addEntity() {
			return Coordinator::addEntity;
		}
	}

	@FunctionalInterface
	public static interface LineCallback {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index);
	}

	@FunctionalInterface
	public static interface LineBiCallback<T> {

		public abstract void run(Coordinator coordinator, int x, int y, int z, int index, T arg);

		//////////////////////////////// internal helper methods ////////////////////////////////

		public static LineBiCallback<CoordinateConsumer> getCoordinates() {
			return (coordinator, x, y, z, index, action) -> coordinator.getCoordinates(x, y, z, action);
		}

		public static LineBiCallback<CoordinateBiConsumer<BlockState>> getBlockState() {
			return (coordinator, x, y, z, index, action) -> coordinator.getBlockState(x, y, z, action);
		}

		public static LineBiCallback<CoordinateBiConsumer<FluidState>> getFluidState() {
			return (coordinator, x, y, z, index, action) -> coordinator.getFluidState(x, y, z, action);
		}

		public static LineBiCallback<CoordinateBiConsumer<BlockEntity>> getBlockEntity() {
			return (coordinator, x, y, z, index, action) -> coordinator.getBlockEntity(x, y, z, action);
		}

		public static LineBiCallback<CoordinateBiConsumer<RegistryEntry<Biome>>> getBiome() {
			return (coordinator, x, y, z, index, action) -> coordinator.getBiome(x, y, z, action);
		}

		public static LineBiCallback<CoordinateBiConsumer<Chunk>> getChunk() {
			return (coordinator, x, y, z, index, action) -> coordinator.getChunk(x, y, z, action);
		}

		public static LineBiCallback<BlockState> setBlockState() {
			return (coordinator, x, y, z, index, state) -> coordinator.setBlockState(x, y, z, state);
		}

		public static LineBiCallback<CoordinateSupplier<BlockState>> setBlockState_supplier() {
			return (coordinator, x, y, z, index, supplier) -> coordinator.setBlockState(x, y, z, supplier);
		}

		public static LineBiCallback<CoordinateSupplier<BlockState>> setBlockStateRelative() {
			return (coordinator, x, y, z, index, supplier) -> coordinator.setBlockStateRelative(x, y, z, supplier);
		}

		public static LineBiCallback<CoordinateUnaryOperator<BlockState>> modifyBlockState() {
			return (coordinator, x, y, z, index, mapper) -> coordinator.modifyBlockState(x, y, z, mapper);
		}

		public static LineBiCallback<CoordinateFunction<ServerWorld, Entity>> addEntity() {
			return (coordinator, x, y, z, index, supplier) -> coordinator.addEntity(x, y, z, supplier);
		}
	}
}