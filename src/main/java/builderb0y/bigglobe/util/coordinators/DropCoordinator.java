package builderb0y.bigglobe.util.coordinators;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

@SuppressWarnings("unchecked")
public class DropCoordinator implements Coordinator {

	public static final DropCoordinator INSTANCE = new DropCoordinator();

	@Override public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {}
	@Override public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {}
	@Override public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {}
	@Override public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {}

	@Override public void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinatorRunnable callback) {}
	@Override public <T> void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, T arg, CoordinatorConsumer<T> callback) {}
	@Override public <A, B> void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {}
	@Override public <A, B, C> void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {}

	@Override public void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, LineRunnable action) {}
	@Override public <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, LineConsumer<T> action, T... args) {}
	@Override public <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, T arg, LineConsumer<T> callback) {}
	@Override public <A, B> void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, A arg1, B arg2, LineBiConsumer<A, B> callback) {}
	@Override public <A, B, C> void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, A arg1, B arg2, C arg3, LineTriConsumer<A, B, C> callback) {}

	@Override public void getCoordinates(int x, int y, int z, CoordinateRunnable action) {}
	@Override public void getCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateRunnable action) {}
	@Override public void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateRunnable action) {}
	@Override public void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, CoordinateRunnable... actions) {}

	@Override public Stream<BlockPos> streamCoordinates(int x, int y, int z) { return Stream.empty(); }
	@Override public Stream<BlockPos> streamCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { return Stream.empty(); }
	@Override public Stream<BlockPos> streamCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length) { return Stream.empty(); }

	@Override public void getBlockState(int x, int y, int z, CoordinateConsumer<BlockState> action) {}
	@Override public void getBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer<BlockState> action) {}
	@Override public void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer<BlockState> action) {}
	@Override public void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer<BlockState>... actions) {}

	@Override public void getFluidState(int x, int y, int z, CoordinateConsumer<FluidState> action) {}
	@Override public void getFluidStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer<FluidState> action) {}
	@Override public void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer<FluidState> action) {}
	@Override public void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer<FluidState>... actions) {}

	@Override public void getBlockEntity(int x, int y, int z, CoordinateConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer<BlockEntity>... actions) {}

	@Override public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateConsumer<B> action) {}
	@Override public <B> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<B> tileEntityType, CoordinateConsumer<B> action) {}
	@Override public <B> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<B> tileEntityType, CoordinateConsumer<B> action) {}

	@Override public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateConsumer<B> action) {}
	@Override public <B extends BlockEntity> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockEntityType<B> tileEntityType, CoordinateConsumer<B> action) {}
	@Override public <B extends BlockEntity> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockEntityType<B> tileEntityType, CoordinateConsumer<B> action) {}

	@Override public void getChunk(int x, int y, int z, CoordinateConsumer<Chunk> action) {}
	@Override public void getChunkCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer<Chunk> action) {}
	@Override public void getChunkLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer<Chunk> action) {}
	@Override public void getChunkLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer<Chunk>... actions) {}

	@Override public void setBlockState(int x, int y, int z, BlockState state) {}
	@Override public void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, BlockState... states) {}
	@Override public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateSupplier<BlockState>... suppliers) {}

	@Override public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) {}
	@Override public <B> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) {}
	@Override public <B> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, Class<B> blockEntityClass, CoordinateConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, BlockEntityType<B> blockEntityType, CoordinateConsumer<B> action) {}

	@Override public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateUnaryOperator<BlockState>... mappers) {}

	@Override public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateConsumer<List<E>> entityAction) {}
	@Override public <E extends Entity> void getEntitiesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateConsumer<List<E>> entityAction) {}
	@Override public <E extends Entity> void getEntitiesLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateConsumer<List<E>> entityAction) {}

	@Override public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateFunction<ServerWorld, Entity>... suppliers) {}

	@Override public Coordinator translate(int offsetX, int offsetY, int offsetZ) { return this; }
	@Override public Coordinator translate(Vec3i offset, boolean lazy) { return this; }
	@Override public Coordinator multiTranslate(int... offsets) { return this; }
	@Override public Coordinator multiTranslate(Vec3i... offsets) { return this; }

	@Override public Coordinator rotate1x(BlockRotation rotation) { return this; }
	@Override public Coordinator rotate4x90() { return this; }
	@Override public Coordinator rotate2x180() { return this; }
	@Override public Coordinator flip1X() { return this; }
	@Override public Coordinator flip1Z() { return this; }
	@Override public Coordinator flip2X() { return this; }
	@Override public Coordinator flip2Z() { return this; }
	@Override public Coordinator flip4XZ() { return this; }

	@Override public Coordinator stack(int dx, int dy, int dz, int count) { return this; }

	@Override public Coordinator limitArea(CoordinateBooleanSupplier predicate) { return this; }
	@Override public Coordinator inBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { return this; }
	@Override public Coordinator inBox(BlockBox box, boolean lazy) { return this; }

	@Override
	public int hashCode() {
		return DropCoordinator.class.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DropCoordinator;
	}

	@Override
	public String toString() {
		return this == INSTANCE ? "DropCoordinator.INSTANCE" : super.toString();
	}
}