package builderb0y.bigglobe.blockEntities;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.features.SerializableBlockQueue;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.DataHelper;
import builderb0y.bigglobe.versions.NbtVersions;

public class DelayedGenerationBlockEntity extends BlockEntity {

	public @Nullable SerializableBlockQueue blockQueue;
	public @Nullable BlockState oldState;
	public @Nullable NbtCompound oldBlockData;

	public DelayedGenerationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public DelayedGenerationBlockEntity(BlockPos pos, BlockState state) {
		this(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state);
	}

	@SuppressWarnings("deprecation")
	public void tick() {
		BlockPos pos = this.pos;
		World world = Objects.requireNonNull(this.world, "world");
		if (this.blockQueue == null) {
			BigGlobeMod.LOGGER.warn("Missing block queue at " + pos);
			world.setBlockState(pos, BlockStates.AIR);
			return;
		}
		if (world.isRegionLoaded(this.blockQueue.minX, this.blockQueue.minY, this.blockQueue.minZ, this.blockQueue.maxX, this.blockQueue.maxY, this.blockQueue.maxZ)) {
			world.setBlockState(pos, this.oldState != null ? this.oldState : BlockStates.AIR);
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
		.begin("oldBlockData").fieldAccessor("oldBlockData", false).codec(NbtCompound.CODEC).add()
	);

	#if MC_VERSION >= MC_1_21_6

		@Override
		public void readData(net.minecraft.storage.ReadView view) {
			super.readData(view);
			DATA_HELPER.read(this, view);
		}

		@Override
		public void writeData(net.minecraft.storage.WriteView view) {
			super.writeData(view);
			DATA_HELPER.write(this, view);
		}

	#else

		@Override
		public void readNbt(NbtCompound nbt #if MC_VERSION >= MC_1_20_5 , RegistryWrapper.WrapperLookup wrapperLookup #endif) {
			super.readNbt(nbt #if MC_VERSION >= MC_1_20_5 , wrapperLookup #endif);
			DATA_HELPER.read(this, nbt);
		}

		@Override
		public void writeNbt(NbtCompound nbt #if MC_VERSION >= MC_1_20_5 , RegistryWrapper.WrapperLookup wrapperLookup #endif) {
			super.writeNbt(nbt #if MC_VERSION >= MC_1_20_5 , wrapperLookup #endif);
			DATA_HELPER.write(this, nbt);
		}

	#endif
}