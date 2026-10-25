package builderb0y.bigglobe.versions;

import builderb0y.bigglobe.BigGlobeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityVersions {

	public static void readFromNbt(BlockEntity blockEntity, CompoundTag nbt) {
		if (BigGlobeMod.currentServer != null) {
			blockEntity.loadWithComponents(nbt, BigGlobeMod.currentServer.registryAccess());
		}
	}

	public static CompoundTag writeToNbt(BlockEntity blockEntity) {
		if (BigGlobeMod.currentServer != null) {
			return blockEntity.saveWithFullMetadata(BigGlobeMod.currentServer.registryAccess());
		}
		return new CompoundTag();
	}

	public static BlockEntity createFromNbt(BlockPos pos, BlockState state, CompoundTag nbt) {
		if (BigGlobeMod.currentServer != null) {
			return BlockEntity.loadStatic(pos, state, nbt, BigGlobeMod.currentServer.registryAccess());
		}
		return null;
	}
}
