package builderb0y.bigglobe.scripting.wrappers;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;

import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper {

	public static final TypeInfo TYPE = type(WorldWrapper.class);

	public final StructureWorldAccess world;
	public final BlockPos.Mutable pos;
	public final Permuter permuter;
	public final WorldColumn biomeColumn;

	public WorldWrapper(StructureWorldAccess world, Permuter permuter) {
		this.world = world;
		this.pos = new BlockPos.Mutable();
		this.permuter = permuter;
		this.biomeColumn = WorldColumn.forWorld(world, 0, 0);
	}

	public long getSeed() {
		return this.world.getSeed();
	}

	public BlockState getBlockState(int x, int y, int z) {
		return this.world.getBlockState(this.pos.set(x, y, z));
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		WorldUtil.setBlockState(this.world, this.pos.set(x, y, z), state, Block.NOTIFY_ALL);
		if (!state.getFluidState().isEmpty()) {
			this.world.scheduleFluidTick(
				this.pos,
				state.getFluidState().getFluid(),
				state.getFluidState().getFluid().getTickRate(this.world)
			);
		}
	}

	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		return SingleBlockFeature.place(this.world, this.pos.set(x, y, z), state, SingleBlockFeature.IS_REPLACEABLE);
	}

	public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		int tmp;
		if (maxX < minX) {
			tmp = minX;
			minX = maxX;
			maxX = tmp;
		}
		if (maxY < minY) {
			tmp = minY;
			minY = maxY;
			maxY = tmp;
		}
		if (maxZ < minZ) {
			tmp = minZ;
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
		return feature.entry().value().generate(
			this.world,
			((ServerChunkManager)(this.world.getChunkManager())).getChunkGenerator(),
			this.permuter.mojang(),
			this.pos.set(x, y, z)
		);
	}

	public BiomeEntry getBiome(int x, int y, int z) {
		this.biomeColumn.setPos(x, z);
		return new BiomeEntry(this.biomeColumn.getBiome(y).vanilla());
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutOfHeightLimit(y);
	}

	public @Nullable NbtCompound getBlockData(int x, int y, int z) {
		BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
		return blockEntity == null ? null : blockEntity.createNbtWithIdentifyingData();
	}

	public void setBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
		if (blockEntity != null) {
			this.doSetBlockData(blockEntity, nbt);
		}
	}

	public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockEntity blockEntity = this.world.getBlockEntity(this.pos.set(x, y, z));
		if (blockEntity != null) {
			NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
			NbtCompound newData = oldData.copy().copyFrom(nbt);
			if (!oldData.equals(newData)) {
				this.doSetBlockData(blockEntity, newData);
			}
		}
	}

	public void doSetBlockData(BlockEntity blockEntity, NbtCompound nbt) {
		blockEntity.readNbt(nbt);
		blockEntity.markDirty();
		if (this.world instanceof World world) {
			BlockState state = this.world.getBlockState(this.pos);
			world.updateListeners(this.pos, state, state, Block.NOTIFY_ALL);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}
}