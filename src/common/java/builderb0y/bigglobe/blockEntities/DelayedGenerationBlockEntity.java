package builderb0y.bigglobe.blockEntities;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.features.SerializableBlockQueue;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.DataHelper;

public class DelayedGenerationBlockEntity extends BlockEntity {

	public @Nullable SerializableBlockQueue blockQueue;
	public @Nullable BlockState oldState;
	public @Nullable CompoundTag oldBlockData;

	public DelayedGenerationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public DelayedGenerationBlockEntity(BlockPos pos, BlockState state) {
		this(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state);
	}

	@SuppressWarnings("deprecation")
	public void tick() {
		BlockPos pos = this.worldPosition;
		Level world = Objects.requireNonNull(this.level, "world");
		if (this.blockQueue == null) {
			BigGlobeMod.LOGGER.warn("Missing block queue at " + pos);
			world.setBlockAndUpdate(pos, BlockStates.AIR);
			return;
		}
		if (world.hasChunksAt(this.blockQueue.minX, this.blockQueue.minY, this.blockQueue.minZ, this.blockQueue.maxX, this.blockQueue.maxY, this.blockQueue.maxZ)) {
			world.setBlockAndUpdate(pos, this.oldState != null ? this.oldState : BlockStates.AIR);
			if (this.oldBlockData != null) {
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (blockEntity != null) BlockEntityVersions.readFromNbt(blockEntity, this.oldBlockData);
			}
			if (!this.blockQueue.hasSpace(world)) {
				return;
			}
			this.blockQueue.actuallyPlaceQueuedBlocks(world);
		}
	}

	public static final DataHelper<DelayedGenerationBlockEntity> DATA_HELPER = (
		new DataHelper<>(DelayedGenerationBlockEntity.class)
			.begin("queue").fieldAccessor("blockQueue", true).add()
			.begin("old_state").fieldAccessor("oldState", true).add()
			.begin("oldBlockData").fieldAccessor("oldBlockData", false).codec(CompoundTag.CODEC).add()
	);

	@Override
	public void loadAdditional(ValueInput view) {
		super.loadAdditional(view);
		DATA_HELPER.read(this, view);
	}

	@Override
	public void saveAdditional(ValueOutput view) {
		super.saveAdditional(view);
		DATA_HELPER.write(this, view);
	}
}