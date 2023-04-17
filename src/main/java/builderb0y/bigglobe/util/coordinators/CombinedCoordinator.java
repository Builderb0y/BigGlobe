package builderb0y.bigglobe.util.coordinators;

import java.util.Arrays;
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

import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class CombinedCoordinator extends ScratchPosCoordinator {

	public final Coordinator[] delegates;

	public CombinedCoordinator(Coordinator... delegates) {
		this.delegates = delegates;
	}

	@Override
	public @Nullable BlockPos getCoordinate(int x, int y, int z) {
		return this.delegates.length == 1 ? this.delegates[0].getCoordinate(x, y, z) : null;
	}

	@Override
	public StructureWorldAccess getWorld() {
		return this.delegates[0].getWorld(); //assume all delegates operate on the same world.
	}

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getWorld(x, y, z, action);
		}
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getCoordinates(x, y, z, action);
		}
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getBlockState(x, y, z, action);
		}
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getFluidState(x, y, z, action);
		}
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getBlockEntity(x, y, z, action);
		}
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getBlockEntity(x, y, z, tileEntityType, action);
		}
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getBlockEntity(x, y, z, tileEntityType, action);
		}
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getBiome(x, y, z, action);
		}
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		for (Coordinator delegate : this.delegates) {
			delegate.getChunk(x, y, z, action);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		for (Coordinator delegate : this.delegates) {
			delegate.setBlockState(x, y, z, state);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		for (Coordinator delegate : this.delegates) {
			delegate.setBlockState(x, y, z, supplier);
		}
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		for (Coordinator delegate : this.delegates) {
			delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
		}
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		for (Coordinator delegate : this.delegates) {
			delegate.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
		}
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		for (Coordinator delegate : this.delegates) {
			delegate.modifyBlockState(x, y, z, mapper);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		for (Coordinator delegate : this.delegates) {
			delegate.getEntities(x, y, z, entityType, boxSupplier, entityAction);
		}
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		for (Coordinator delegate : this.delegates) {
			delegate.addEntity(x, y, z, supplier);
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.delegates);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof CombinedCoordinator)) return false;
		CombinedCoordinator that = (CombinedCoordinator)(obj);
		return Arrays.equals(this.delegates, that.delegates);
	}

	@Override
	public String toString() {
		return Arrays.toString(this.delegates);
	}
}