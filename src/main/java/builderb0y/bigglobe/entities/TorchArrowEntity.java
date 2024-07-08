package builderb0y.bigglobe.entities;

import org.jetbrains.annotations.Nullable;

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
import builderb0y.bigglobe.versions.EntityVersions;

public class TorchArrowEntity extends PersistentProjectileEntity {

	public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world #if MC_VERSION >= MC_1_20_3 && MC_VERSION < MC_1_21_0 , BigGlobeItems.TORCH_ARROW.getDefaultStack() #endif);
	}

	#if MC_VERSION >= MC_1_21_0

		public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> type, double x, double y, double z, World world, ItemStack stack, @Nullable ItemStack weapon) {
			super(type, x, y, z, world, stack, weapon);
		}

		public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> type, LivingEntity owner, World world, ItemStack stack, @Nullable ItemStack shotFrom) {
			super(type, owner, world, stack, shotFrom);
		}
	#else

		public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world, LivingEntity owner) {
			super(entityType, owner, world #if MC_VERSION >= MC_1_20_3 , BigGlobeItems.TORCH_ARROW.getDefaultStack() #endif);
		}

		public TorchArrowEntity(EntityType<? extends PersistentProjectileEntity> type, double x, double y, double z, World world) {
			super(type, x, y, z, world #if MC_VERSION >= MC_1_20_3 , BigGlobeItems.TORCH_ARROW.getDefaultStack() #endif);
		}
	#endif

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
		World world = EntityVersions.getWorld(this);
		if (!world.isClient) {
			BlockState hitState = world.getBlockState(blockHitResult.getBlockPos());
			if (hitState.getBlock() instanceof TntBlock) {
				this.setOnFireFor(1); //TNT ignites when a flaming projectile hits it.
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
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
			if (toPlace != null && this.tryPlace(blockHitResult, toPlace)) {
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
				this.discard();
				return;
			}
			//secondary action: bounce off the block.
			//this action is most likely to fail when we hit the block face directly,
			//and most likely to succeed when we hit the block at a glancing angle.
			if (world.random.nextDouble() >= this.getBounceFailChance(blockHitResult)) {
				Vec3d velocity = this.getVelocity();
				Axis axis = blockHitResult.getSide().getAxis();
				this.setVelocity(
					(axis == Axis.X ? -0.25D : 0.75D) * velocity.x,
					(axis == Axis.Y ? -0.25D : 0.75D) * velocity.y,
					(axis == Axis.Z ? -0.25D : 0.75D) * velocity.z
				);
				this.velocityDirty = true;
				this.playSound(this.getSound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
				hitState.onProjectileHit(world, hitState, blockHitResult, this);
				this.setCritical(false);
				this.setPierceLevel((byte)(0));
				return;
			}
		}
		//last action: get stuck.
		super.onBlockHit(blockHitResult);
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
		World world = EntityVersions.getWorld(this);
		if (
			world.isAir(placementPos) &&
			toPlace.canPlaceAt(world, placementPos) &&
			world.setBlockState(placementPos, toPlace)
		) {
			BlockSoundGroup sound = toPlace.getSoundGroup();
			world.playSound(null, placementPos, sound.getPlaceSound(), SoundCategory.BLOCKS, sound.getVolume() * 0.5F + 0.5F, sound.getPitch() * 0.8f);
			return true;
		}
		return false;
	}

	#if MC_VERSION < MC_1_20_3

		@Override
		public ItemStack asItemStack() {
			return new ItemStack(BigGlobeItems.TORCH_ARROW);
		}
	#endif

	#if MC_VERSION >= MC_1_20_5

		@Override
		public ItemStack getDefaultItemStack() {
			return new ItemStack(BigGlobeItems.TORCH_ARROW);
		}
	#endif
}