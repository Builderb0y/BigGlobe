package builderb0y.bigglobe.util.coordinators;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.registry.RegistryEntry;
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

	@Override public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {}
	@Override public void getCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer action) {}
	@Override public void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer action) {}
	@Override public void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer... actions) {}
	@Override public Stream<BlockPos> streamCoordinates(int x, int y, int z) { return Stream.empty(); }
	@Override public Stream<BlockPos> streamCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { return Stream.empty(); }
	@Override public Stream<BlockPos> streamCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length) { return Stream.empty(); }
	@Override public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {}
	@Override public void getBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<BlockState> action) {}
	@Override public void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<BlockState> action) {}
	@Override public void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<BlockState>... actions) {}
	@Override public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {}
	@Override public void getFluidStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<FluidState> action) {}
	@Override public void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<FluidState> action) {}
	@Override public void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<FluidState>... actions) {}
	@Override public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<BlockEntity> action) {}
	@Override public void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<BlockEntity>... actions) {}
	@Override public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, Class<B> tileEntityType, CoordinateBiConsumer<B>... actions) {}
	@Override public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B>... actions) {}
	@Override public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {}
	@Override public void getBiomeCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<RegistryEntry<Biome>> action) {}
	@Override public void getBiomeLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<RegistryEntry<Biome>> action) {}
	@Override public void getBiomeLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<RegistryEntry<Biome>>... actions) {}
	@Override public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {}
	@Override public void getChunkCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<Chunk> action) {}
	@Override public void getChunkLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<Chunk> action) {}
	@Override public void getChunkLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<Chunk>... actions) {}
	@Override public void setBlockState(int x, int y, int z, BlockState state) {}
	@Override public void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, BlockState... states) {}
	@Override public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateSupplier<BlockState>... suppliers) {}
	@Override public void setBlockStateRelative(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateRelativeCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateRelativeLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateSupplier<BlockState> supplier) {}
	@Override public void setBlockStateRelativeLine(int x, int y, int z, int dx, int dy, int dz, CoordinateSupplier<BlockState>... suppliers) {}
	@Override public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {}
	@Override public <B> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {}
	@Override public <B> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {}
	@Override public <B extends BlockEntity> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {}
	@Override public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateUnaryOperator<BlockState> mapper) {}
	@Override public void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateUnaryOperator<BlockState>... mappers) {}
	@Override public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {}
	@Override public <E extends Entity> void getEntitiesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {}
	@Override public <E extends Entity> void getEntitiesLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {}
	@Override public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateFunction<ServerWorld, Entity> supplier) {}
	@Override public void addEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateFunction<ServerWorld, Entity>... suppliers) {}
	@Override public void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CuboidCallback action) {}
	@Override public <T> void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, T arg, CuboidBiCallback<T> action) {}
	@Override public void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, LineCallback action) {}
	@Override public <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, LineBiCallback<T> action, T... args) {}
	@Override public <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, T arg, LineBiCallback<T> action) {}
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
	@Override public Coordinator limitArea(CoordinatePredicate predicate) { return this; }
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