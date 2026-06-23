package builderb0y.bigglobe.blockEntities;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.features.SizedDelayedFeatureConfig;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.DataHelper;
import builderb0y.bigglobe.versions.RegistryVersions;

public class DelayedGenerationBlockEntity extends BlockEntity {

	public @Nullable Identifier feature;
	public @Nullable BlockState oldState;
	public @Nullable CompoundTag oldBlockData;

	public DelayedGenerationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public DelayedGenerationBlockEntity(BlockPos pos, BlockState state) {
		this(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state);
	}

	public void revert(ServerLevel world) {
		if (this.oldState != null) {
			world.setBlock(this.worldPosition, this.oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			if (this.oldBlockData != null) {
				BlockEntity blockEntity = world.getBlockEntity(this.worldPosition);
				if (blockEntity != null) BlockEntityVersions.readFromNbt(blockEntity, this.oldBlockData);
			}
		}
		else {
			world.setBlock(this.worldPosition, BlockStates.AIR, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	public void tick() {
		BlockPos pos = this.worldPosition;
		if (!(this.level instanceof ServerLevel world)) {
			return;
		}
		if (this.feature == null) {
			BigGlobeMod.LOGGER.warn("Missing feature at " + pos);
			this.revert(world);
			return;
		}
		ConfiguredFeature<?, ?> feature = RegistryVersions.getRegistry(world.registryAccess(), Registries.CONFIGURED_FEATURE).getOptional(this.feature).orElse(null);
		if (feature == null) {
			BigGlobeMod.LOGGER.warn("Unknown feature " + this.feature + " at " + this.worldPosition);
			this.revert(world);
			return;
		}
		int range;
		if (feature.config() instanceof SizedDelayedFeatureConfig sizedConfig) {
			range = sizedConfig.getMaxRadiusInBlocks();
		}
		else {
			range = 16;
		}
		range++; //ensure space for updating neighbors too.

		if (WorldUtil.isAreaLoaded(
			world,
			new BoundingBox(
				pos.getX() - range,
				pos.getY() - range,
				pos.getZ() - range,
				pos.getX() + range,
				pos.getY() + range,
				pos.getZ() + range
			)
		)) {
			this.revert(world);
			feature.feature().place(new FeaturePlaceContext(Optional.of(feature), world, world.getChunkSource().getGenerator(), RandomSource.create(Permuter.permute(world.getSeed() ^ 0xc99e62371fba6cecL, pos)), pos, feature.config()));
		}
	}

	public static final DataHelper<DelayedGenerationBlockEntity> DATA_HELPER = (
		new DataHelper<>(DelayedGenerationBlockEntity.class)
		.begin("feature").fieldAccessor("feature", true).add()
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