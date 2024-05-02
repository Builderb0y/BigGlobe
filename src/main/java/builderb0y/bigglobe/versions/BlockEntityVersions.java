package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.BigGlobeMod;

public class BlockEntityVersions {

	public static void readFromNbt(BlockEntity blockEntity, NbtCompound nbt) {
		#if MC_VERSION >= MC_1_20_5
			blockEntity.read(nbt, BigGlobeMod.getCurrentServer().getRegistryManager());
		#else
			blockEntity.readNbt(nbt);
		#endif
	}

	public static NbtCompound writeToNbt(BlockEntity blockEntity) {
		#if MC_VERSION >= MC_1_20_5
			return blockEntity.createNbtWithIdentifyingData(BigGlobeMod.getCurrentServer().getRegistryManager());
		#else
			return BlockEntity.writeNbt();
		#endif
	}

	public static BlockEntity createFromNbt(BlockPos pos, BlockState state, NbtCompound nbt) {
		return BlockEntity.createFromNbt(pos, state, nbt #if MC_VERSION >= MC_1_20_5 , BigGlobeMod.getCurrentServer().getRegistryManager() #endif);
	}
}