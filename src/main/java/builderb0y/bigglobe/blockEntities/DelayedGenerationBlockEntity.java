package builderb0y.bigglobe.blockEntities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.features.SerializableBlockQueue;

public class DelayedGenerationBlockEntity extends BlockEntity {

	public SerializableBlockQueue blockQueue;

	public DelayedGenerationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public void tick() {
		if (this.blockQueue == null) {
			BigGlobeMod.LOGGER.warn("Missing block queue at " + this.pos);
			this.world.setBlockState(this.pos, BlockStates.AIR);
			return;
		}
		if (!this.blockQueue.hasSpace(this.world)) {
			this.world.setBlockState(this.pos, BlockStates.AIR);
			return;
		}
		this.blockQueue.actuallyPlaceQueuedBlocks(this.world);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		try {
			this.blockQueue = SerializableBlockQueue.read(nbt.getCompound("queue"));
		}
		catch (RuntimeException exception) {
			BigGlobeMod.LOGGER.error("Error reading NBT data for delayed generation at " + this.pos, exception);
		}
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.put("queue", this.blockQueue.toNBT());
	}
}