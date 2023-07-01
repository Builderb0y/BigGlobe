package builderb0y.bigglobe.util.coordinators;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder.ColumnLookup;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class ChunkCoordinator extends ScratchPosCoordinator {

	public final Chunk chunk;
	public final ColumnLookup biomeColumn;

	public ChunkCoordinator(Chunk chunk, ColumnLookup biomeColumn) {
		this.chunk = chunk;
		this.biomeColumn = biomeColumn;
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		action.accept(this.scratchPos.set(x, y, z));
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk.getBlockState(this.scratchPos));
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk.getFluidState(this.scratchPos));
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		BlockEntity blockEntity = this.chunk.getBlockEntity(this.scratchPos.set(x, y, z));
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> blockEntityType, CoordinateBiConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		action.accept(
			this.scratchPos.set(x, y, z),
			this.biomeColumn.lookupColumn(x, z).getBiome(y)
		);
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		action.accept(this.scratchPos.set(x, y, z), this.chunk);
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		this.chunk.setBlockState(this.scratchPos.set(x, y, z), state, false);
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (supplier == null) return;
		BlockState state = supplier.get(this.scratchPos.set(x, y, z));
		if (state == null) return;
		this.chunk.setBlockState(this.scratchPos.set(x, y, z), state, false);
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.chunk.setBlockState(this.scratchPos.set(x, y, z), state, false);
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityClass);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.chunk.setBlockState(this.scratchPos.set(x, y, z), state, false);
		B blockEntity = WorldUtil.getBlockEntity(this.chunk, this.scratchPos.set(x, y, z), blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		BlockState oldState = this.chunk.getBlockState(this.scratchPos.set(x, y, z));
		BlockState newState = mapper.apply(this.scratchPos, oldState);
		if (newState != oldState && newState != null) {
			this.chunk.setBlockState(this.scratchPos.set(x, y, z), newState, false);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		throw new UnsupportedOperationException("Chunks don't store entities.");
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
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