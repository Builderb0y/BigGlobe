package builderb0y.bigglobe.entities;

import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.EntityVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TorchArrowEntity extends AbstractArrow {

	public TorchArrowEntity(EntityType<? extends AbstractArrow> entityType, Level world) {
		super(entityType, world);
	}

	public TorchArrowEntity(EntityType<? extends AbstractArrow> type, double x, double y, double z, Level world, ItemStack stack, @Nullable ItemStack weapon) {
		super(type, x, y, z, world, stack, weapon);
	}

	public TorchArrowEntity(EntityType<? extends AbstractArrow> type, LivingEntity owner, Level world, ItemStack stack, @Nullable ItemStack shotFrom) {
		super(type, owner, world, stack, shotFrom);
	}

	@Override
	public void doPostHurtEffects(LivingEntity target) {
		super.doPostHurtEffects(target);
		target.igniteForSeconds(3);
		if (target instanceof Creeper creeper) {
			creeper.ignite();
		}
	}

	@Override
	public void onHitBlock(BlockHitResult blockHitResult) {
		Level world = EntityVersions.getWorld(this);
		if (!world.isClientSide()) {
			BlockState hitState = world.getBlockState(blockHitResult.getBlockPos());
			if (hitState.getBlock() instanceof TntBlock) {
				this.igniteForSeconds(1); //TNT ignites when a flaming projectile hits it.
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
				this.discard();
				return;
			}
			BlockState toPlace = switch (blockHitResult.getDirection()) {
				case UP -> Blocks.TORCH.defaultBlockState();
				case DOWN -> null;
				case EAST, WEST, NORTH, SOUTH -> Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, blockHitResult.getDirection());
			};
			//primary action: place a torch.
			//this requires that a torch can be placed on the side of the block we hit
			//(in other words, we weren't traveling up and hit the bottom of a block),
			//and that a torch can be placed at this location
			//(in other words, there isn't already another block here).
			//this action is most likely to succeed when you hit the center of the block.
			//see also: getPlacementFailChance().
			if (toPlace != null && this.tryPlace(blockHitResult, toPlace)) {
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
				this.discard();
				return;
			}
			//secondary action: bounce off the block.
			//this action is most likely to fail when we hit the block face directly,
			//and most likely to succeed when we hit the block at a glancing angle.
			if (world.random.nextDouble() >= this.getBounceFailChance(blockHitResult)) {
				Vec3 velocity = this.getDeltaMovement();
				Axis axis = blockHitResult.getDirection().getAxis();
				this.setDeltaMovement(
					(axis == Axis.X ? -0.25D : 0.75D) * velocity.x,
					(axis == Axis.Y ? -0.25D : 0.75D) * velocity.y,
					(axis == Axis.Z ? -0.25D : 0.75D) * velocity.z
				);
				this.needsSync = true;
				this.playSound(this.getHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
				this.setCritArrow(false);
				this.setPierceLevel((byte)(0));
				return;
			}
		}
		//last action: get stuck.
		super.onHitBlock(blockHitResult);
	}

	public double getBounceFailChance(BlockHitResult blockHitResult) {
		Vec3 velocity = this.getDeltaMovement().normalize();
		Direction face = blockHitResult.getDirection();
		return BigGlobeMath.squareD(
			velocity.x * face.getStepX() +
			velocity.y * face.getStepY() +
			velocity.z * face.getStepZ()
		);
	}

	public boolean tryPlace(BlockHitResult blockHitResult, BlockState toPlace) {
		BlockPos placementPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
		Level world = EntityVersions.getWorld(this);
		if (
			world.isEmptyBlock(placementPos) &&
			toPlace.canSurvive(world, placementPos) &&
			world.setBlockAndUpdate(placementPos, toPlace)
		) {
			SoundType sound = toPlace.getSoundType();
			world.playSound(null, placementPos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume() * 0.5F + 0.5F, sound.getPitch() * 0.8f);
			return true;
		}
		return false;
	}

	@Override
	public ItemStack getDefaultPickupItem() {
		return new ItemStack(BigGlobeItems.TORCH_ARROW);
	}
}