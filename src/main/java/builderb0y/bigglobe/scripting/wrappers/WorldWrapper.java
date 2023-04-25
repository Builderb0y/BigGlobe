package builderb0y.bigglobe.scripting.wrappers;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper {

	public static final TypeInfo TYPE = type(WorldWrapper.class);

	public final StructureWorldAccess world;
	public final Coordinator coordinator;
	public final Permuter permuter;
	public final WorldColumn biomeColumn;

	public WorldWrapper(StructureWorldAccess world, Coordinator coordinator, Permuter permuter) {
		this.world = world;
		this.coordinator = coordinator;
		this.permuter = permuter;
		this.biomeColumn = WorldColumn.forWorld(coordinator.getWorld(), 0, 0);
	}

	public @Nullable BlockPos pos(int x, int y, int z) {
		return this.coordinator.getCoordinate(x, y, z);
	}

	public long getSeed() {
		return this.world.getSeed();
	}

	public BlockState getBlockState(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		return pos != null ? this.world.getBlockState(pos) : BlockStates.AIR;
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			WorldUtil.setBlockState(this.world, pos, state, Block.NOTIFY_ALL);
			if (!state.getFluidState().isEmpty()) {
				this.world.scheduleFluidTick(
					pos,
					state.getFluidState().getFluid(),
					state.getFluidState().getFluid().getTickRate(this.world)
				);
			}
		}
	}

	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.pos(x, y, z);
		return pos != null && SingleBlockFeature.place(this.world, pos, state, SingleBlockFeature.IS_REPLACEABLE);
	}

	public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		int tmp;
		if (maxX < minX) {
			tmp  = minX;
			minX = maxX;
			maxX = tmp;
		}
		if (maxY < minY) {
			tmp  = minY;
			minY = maxY;
			maxY = tmp;
		}
		if (maxZ < minZ) {
			tmp  = minZ;
			minZ = maxZ;
			maxZ = tmp;
		}
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					this.setBlockState(x, y, z, state);
				}
			}
		}
	}

	public boolean placeFeature(int x, int y, int z, ConfiguredFeatureEntry feature) {
		BlockPos pos = this.pos(x, y, z);
		return pos != null && feature.object().generate(
			this.world,
			((ServerChunkManager)(this.world.getChunkManager())).getChunkGenerator(),
			this.permuter.mojang(),
			pos
		);
	}

	public BiomeEntry getBiome(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			this.biomeColumn.setPos(x, z);
			return new BiomeEntry(this.biomeColumn.getBiome(y));
		}
		else {
			return new BiomeEntry(
				this
				.world
				.getRegistryManager()
				.get(RegistryKeys.BIOME)
				.getEntry(BiomeKeys.PLAINS)
				.orElseThrow(() -> new UnregisteredObjectException("Missing default plains biome"))
			);
		}
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutOfHeightLimit(y);
	}

	public boolean isPositionValid(int x, int y, int z) {
		return this.isYLevelValid(y) && this.pos(x, y, z) != null;
	}

	public @Nullable NbtCompound getBlockData(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				return blockEntity.createNbtWithIdentifyingData();
			}
		}
		return null;
	}

	public void setBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				this.doSetBlockData(pos, blockEntity, nbt);
			}
		}
	}

	public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
				NbtCompound newData = oldData.copy().copyFrom(nbt);
				if (!oldData.equals(newData)) {
					this.doSetBlockData(pos, blockEntity, newData);
				}
			}
		}
	}

	public void doSetBlockData(BlockPos pos, BlockEntity blockEntity, NbtCompound nbt) {
		blockEntity.readNbt(nbt);
		blockEntity.markDirty();
		if (this.world instanceof World world) {
			BlockState state = this.world.getBlockState(pos);
			world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}
}