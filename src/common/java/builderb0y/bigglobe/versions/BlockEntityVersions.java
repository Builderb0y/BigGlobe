package builderb0y.bigglobe.versions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import builderb0y.bigglobe.BigGlobeMod;

public class BlockEntityVersions {

	public static ValueInput readView(CompoundTag nbt) {
		return TagValueInput.create(ProblemReporter.DISCARDING, BigGlobeMod.getCurrentServer().registryAccess(), nbt);
	}

	public static void readFromNbt(BlockEntity blockEntity, CompoundTag nbt) {

		blockEntity.loadWithComponents(readView(nbt));
	}

	public static CompoundTag writeToNbt(BlockEntity blockEntity) {

		return blockEntity.saveWithFullMetadata(BigGlobeMod.getCurrentServer().registryAccess());
	}

	public static BlockEntity createFromNbt(BlockPos pos, BlockState state, CompoundTag nbt) {
		return BlockEntity.loadStatic(pos, state, nbt, BigGlobeMod.getCurrentServer().registryAccess());
	}
}