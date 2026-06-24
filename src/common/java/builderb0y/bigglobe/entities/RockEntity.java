package builderb0y.bigglobe.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import builderb0y.bigglobe.blockdefs.BigGlobeBlockTags;
import builderb0y.bigglobe.blockdefs.OverworldBlocks;
import builderb0y.bigglobe.blocks.RockBlock;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.EntityVersions;

public class RockEntity extends ThrowableItemProjectile {

	public RockEntity(EntityType<? extends RockEntity> entityType, Level world) {
		super(entityType, world);
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, double d, double e, double f, Level world) {
		super(entityType, d, e, f, world, new ItemStack(BigGlobeItems.ROCK));
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, LivingEntity livingEntity, Level world) {
		super(entityType, livingEntity, world, new ItemStack(BigGlobeItems.ROCK));
	}

	@Override
	public Item getDefaultItem() {
		return BigGlobeItems.ROCK;
	}

	@Override
	public void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		if (EntityVersions.getWorld(this) instanceof ServerLevel serverWorld) {
			entityHitResult.getEntity().hurtServer(

				serverWorld,

				this.damageSources().thrown(this, this.getOwner()),
				(float)(this.getDeltaMovement().length() * 6.0D)
			);
		}
		this.discard();
	}

	@Override
	public void tick() {
		BlockHitResult bounceCollision = this.getBounceCollision();
		if (bounceCollision != null && bounceCollision.getType() != HitResult.Type.MISS) {
			this.tryBounceCollision(bounceCollision);
		}
		super.tick();
	}

	public BlockHitResult getBounceCollision() {
		Level world = EntityVersions.getWorld(this);
		FluidState fluidState = world.getFluidState(BlockPos.containing(this.getX(), this.getY() + 0.125D, this.getZ()));
		Vec3 velocity = this.getDeltaMovement();
		Vec3 position = EntityVersions.getPos(this);
		return world.clip(
			new ClipContext(
				position,
				position.add(velocity),
				Block.COLLIDER,
				fluidState.isEmpty()
					? Fluid.ANY
					: Fluid.NONE,
				this
			)
		);
	}

	public void tryBounceCollision(BlockHitResult blockHitResult) {
		BlockState hitState = EntityVersions.getWorld(this).getBlockState(blockHitResult.getBlockPos());
		if (!hitState.getFluidState().isEmpty()) {
			if (hitState.getFluidState().is(FluidTags.WATER)) {
				if (
					blockHitResult.getDirection() == Direction.UP &&
					this.getDeltaMovement().horizontalDistanceSqr() >= BigGlobeMath.squareD(this.getDeltaMovement().y) * 3.0D
				) {
					this.bounce(blockHitResult, true);
				}
				//else go through surface
			}
			else if (hitState.getFluidState().is(FluidTags.LAVA)) {
				if (EntityVersions.getWorld(this) instanceof ServerLevel world) {
					world.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 16, 0.0D, 0.0D, 0.0D, 0.0D);
					world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 1.0F, 1.0F);
				}
				this.discard();
			}
		}
		else if (
				blockHitResult.getDirection() == Direction.UP &&
			this.getDeltaMovement().lengthSqr() < 0.125D * 0.125D
		) {
			this.placeRock(blockHitResult);
		}
		else {
			if (hitState.is(BigGlobeBlockTags.ROCK_BREAKABLE)) {
				EntityVersions.getWorld(this).destroyBlock(blockHitResult.getBlockPos(), true, this);
				this.setDeltaMovement(this.getDeltaMovement().scale(0.75D));
			}
			else {
				this.bounce(blockHitResult, false);
			}
		}
	}

	public void placeRock(BlockHitResult blockHitResult) {
		BlockPos placePos = blockHitResult.getBlockPos().above();
		BlockState existingState = EntityVersions.getWorld(this).getBlockState(placePos);
		int rocks;
		if (existingState.is(OverworldBlocks.ROCK) && (rocks = existingState.getValue(RockBlock.ROCKS)) < 6) {
			EntityVersions.getWorld(this).setBlockAndUpdate(placePos, existingState.setValue(RockBlock.ROCKS, rocks + 1));
		}
		else {
			SingleBlockFeature.place(EntityVersions.getWorld(this), placePos, OverworldBlocks.ROCK.defaultBlockState(), SingleBlockFeature.IS_REPLACEABLE);
		}
		SoundType group = SoundType.STONE;
		EntityVersions.getWorld(this).playSound(null, placePos, group.getBreakSound(), SoundSource.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.8F);
		this.discard();
	}

	public void bounce(BlockHitResult blockHitResult, boolean water) {
		Vec3 velocity = this.getDeltaMovement();
		Axis axis = blockHitResult.getDirection().getAxis();
		this.setDeltaMovement(
			(axis == Axis.X ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.x,
			(axis == Axis.Y ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.y,
			(axis == Axis.Z ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.z
		);
		this.needsSync = true;
		if (water) {
			EntityVersions.getWorld(this).playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
		}
		else {
			SoundType group = SoundType.STONE;
			EntityVersions.getWorld(this).playSound(null, this.getX(), this.getY(), this.getZ(), group.getHitSound(), SoundSource.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.5F);
		}
	}

	@Override
	public double getDefaultGravity() {
		return 0.05F;
	}
}