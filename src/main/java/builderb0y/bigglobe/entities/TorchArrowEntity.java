package builderb0y.bigglobe.entities;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TntBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;

public class TorchArrowEntity extends PersistentProjectileEntity {

	public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world, LivingEntity owner) {
		super(entityType, owner, world);
	}

	@Override
	public void onHit(LivingEntity target) {
		super.onHit(target);
		target.setOnFireFor(3);
		if (target instanceof CreeperEntity creeper) {
			creeper.ignite();
		}
	}

	@Override
	public void onBlockHit(BlockHitResult blockHitResult) {
		if (!this.world.isClient) {
			BlockState hitState = this.world.getBlockState(blockHitResult.getBlockPos());
			if (hitState.getBlock() instanceof TntBlock) {
				this.setOnFireFor(1); //TNT ignites when a flaming projectile hits it.
				hitState.onProjectileHit(this.world, hitState, blockHitResult, this);
				this.discard();
				return;
			}
			BlockState toPlace = switch (blockHitResult.getSide()) {
				case UP -> Blocks.TORCH.getDefaultState();
				case DOWN -> null;
				case EAST, WEST, NORTH, SOUTH -> Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, blockHitResult.getSide());
			};
			//primary action: place a torch.
			//this requires that a torch can be placed on the side of the block we hit
			//(in other words, we weren't traveling up and hit the bottom of a block),
			//and that a torch can be placed at this location
			//(in other words, there isn't already another block here).
			//this action is most likely to succeed when you hit the center of the block.
			//see also: getPlacementFailChance().
			if (
				toPlace != null &&
				this.world.random.nextDouble() >= this.getPlacementFailChance(blockHitResult) &&
				this.tryPlace(blockHitResult, toPlace)
			) {
				hitState.onProjectileHit(this.world, hitState, blockHitResult, this);
				this.discard();
				return;
			}
			//secondary action: bounce off the block.
			//this action is most likely to fail when we hit the block face directly,
			//and most likely to succeed when we hit the block at a glancing angle.
			if (this.world.random.nextDouble() >= this.getBounceFailChance(blockHitResult)) {
				Vec3d velocity = this.getVelocity();
				Axis axis = blockHitResult.getSide().getAxis();
				this.setVelocity(
					(axis == Axis.X ? -0.25D : 0.75D) * velocity.x,
					(axis == Axis.Y ? -0.25D : 0.75D) * velocity.y,
					(axis == Axis.Z ? -0.25D : 0.75D) * velocity.z
				);
				this.velocityDirty = true;
				this.playSound(this.getSound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
				hitState.onProjectileHit(this.world, hitState, blockHitResult, this);
				this.setCritical(false);
				this.setPierceLevel((byte)(0));
				return;
			}
		}
		//last action: get stuck.
		super.onBlockHit(blockHitResult);
	}

	/**
	in a nutshell, this method determines how far away from the center of the block you hit.
	the closer you are to hitting the center of the block,
	the more likely it is that a torch will be placed.
	the chance of placing a torch when you hit the center of the block is 100%.
	the chance of placing a torch when you hit the edge of the block is 50%.
	*/
	public double getPlacementFailChance(BlockHitResult blockHitResult) {
		Vec3d pos = blockHitResult.getPos();
		BlockPos floorPos = blockHitResult.getBlockPos();
		double offsetX = Math.abs(pos.getX() - floorPos.getX() - 0.5D);
		double offsetY = Math.abs(pos.getY() - floorPos.getY() - 0.5D);
		double offsetZ = Math.abs(pos.getZ() - floorPos.getZ() - 0.5D);
		return switch (blockHitResult.getSide().getAxis()) {
			case X -> Math.max(offsetY, offsetZ);
			case Y -> Math.max(offsetX, offsetZ);
			case Z -> Math.max(offsetX, offsetY);
		};
	}

	public double getBounceFailChance(BlockHitResult blockHitResult) {
		Vec3d velocity = this.getVelocity().normalize();
		Direction face = blockHitResult.getSide();
		return BigGlobeMath.squareD(
			velocity.x * face.getOffsetX() +
			velocity.y * face.getOffsetY() +
			velocity.z * face.getOffsetZ()
		);
	}

	public boolean tryPlace(BlockHitResult blockHitResult, BlockState toPlace) {
		BlockPos placementPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
		if (
			this.world.isAir(placementPos) &&
			toPlace.canPlaceAt(this.world, placementPos) &&
			this.world.setBlockState(placementPos, toPlace)
		) {
			BlockSoundGroup sound = toPlace.getSoundGroup();
			this.world.playSound(null, placementPos, sound.getPlaceSound(), SoundCategory.BLOCKS, sound.getVolume() * 0.5F + 0.5F, sound.getPitch() * 0.8f);
			return true;
		}
		return false;
	}

	@Override
	public ItemStack asItemStack() {
		return new ItemStack(BigGlobeItems.TORCH_ARROW);
	}
}