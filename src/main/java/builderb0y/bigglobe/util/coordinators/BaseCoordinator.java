package builderb0y.bigglobe.util.coordinators;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class BaseCoordinator extends ScratchPosCoordinator {

	public final StructureWorldAccess world;
	public final int setBlockFlags;
	public WorldColumn column;

	public BaseCoordinator(StructureWorldAccess world, int setBlockFlags) {
		this.world = world;
		this.setBlockFlags = setBlockFlags;
	}

	@Override
	public @Nullable BlockPos getCoordinate(int x, int y, int z) {
		return this.scratchPos.set(x, y, z);
	}

	@Override
	public StructureWorldAccess getWorld() {
		return this.world;
	}

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world);
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		action.accept(this.scratchPos.set(x, y, z));
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getBlockState(this.scratchPos));
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getFluidState(this.scratchPos));
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		BlockEntity tileEntity = this.world.getBlockEntity(this.scratchPos.set(x, y, z));
		if (tileEntity != null) action.accept(this.scratchPos, tileEntity);
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		B tileEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos.set(x, y, z), tileEntityType);
		if (tileEntity != null) action.accept(this.scratchPos, tileEntity);
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		B tileEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos.set(x, y, z), tileEntityType);
		if (tileEntity != null) action.accept(this.scratchPos, tileEntity);
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		WorldColumn column = this.column;
		if (column == null) {
			column = this.column = WorldColumn.forWorld(this.world, 0, 0);
		}
		column.setPos(x, z);
		action.accept(this.scratchPos.set(x, y, z), column.getBiome(y));
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		action.accept(this.scratchPos.set(x, y, z), this.world.getChunk(x >> 4, z >> 4));
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		this.world.setBlockState(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		BlockState state = supplier.get(this.scratchPos.set(x, y, z));
		if (state != null) this.world.setBlockState(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.world.setBlockState(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos, blockEntityClass);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		this.world.setBlockState(this.scratchPos.set(x, y, z), state, this.setBlockFlags);
		B blockEntity = WorldUtil.getBlockEntity(this.world, this.scratchPos, blockEntityType);
		if (blockEntity != null) action.accept(this.scratchPos, blockEntity);
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		BlockState oldState = this.world.getBlockState(this.scratchPos.set(x, y, z));
		BlockState newState = mapper.apply(this.scratchPos, oldState);
		if (oldState != newState && newState != null) {
			this.world.setBlockState(this.scratchPos.set(x, y, z), newState, this.setBlockFlags);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		Box box = boxSupplier.get(this.scratchPos.set(x, y, z));
		entityAction.accept(this.scratchPos.set(x, y, z), this.world.getNonSpectatingEntities(entityType, box));
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		Entity entity = supplier.apply(this.scratchPos.set(x, y, z), this.world.toServerWorld());
		if (entity != null) this.world.spawnEntity(entity);
	}

	@Override
	public int hashCode() {
		return this.world.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof BaseCoordinator && this.world.equals(((BaseCoordinator)(that)).world);
	}

	@Override
	public String toString() {
		return "BaseCoordinator: { " + this.world + " }";
	}
}