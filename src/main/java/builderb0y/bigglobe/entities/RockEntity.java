package builderb0y.bigglobe.entities;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;

public class RockEntity extends ThrownItemEntity {

	public RockEntity(EntityType<? extends RockEntity> entityType, World world) {
		super(entityType, world);
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, double d, double e, double f, World world) {
		super(entityType, d, e, f, world);
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, LivingEntity livingEntity, World world) {
		super(entityType, livingEntity, world);
	}

	@Override
	public Item getDefaultItem() {
		return BigGlobeItems.ROCK;
	}

	@Override
	public void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		entityHitResult.getEntity().damage(
			DamageSource.thrownProjectile(this, this.getOwner()),
			(float)(this.getVelocity().length() * 6.0D)
		);
		this.discard();
	}

	/**
	mostly a copy-paste of {@link ProjectileUtil#getCollision(Entity, Predicate)},
	but allowing collisions with fluids when this rock is not in a fluid.
	*/
	@SuppressWarnings("unused")
	public HitResult bigglobe_getCollision() {
		EntityHitResult entityHitResult;
		Vec3d nextPosition;
		Vec3d velocity = this.getVelocity();
		World world = this.world;
		Vec3d position = this.getPos();
		HitResult blockHitResult = world.raycast(
			new RaycastContext(
				position,
				nextPosition = position.add(velocity),
				ShapeType.COLLIDER,
				this.world.getBlockState(new BlockPos(this.getX(), this.getY() + 0.125D, this.getZ())).getBlock() == Blocks.WATER
				? FluidHandling.NONE
				: FluidHandling.ANY,
				this
			)
		);
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			nextPosition = blockHitResult.getPos();
		}
		if ((entityHitResult = ProjectileUtil.getEntityCollision(world, this, position, nextPosition, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0), this::canHit)) != null) {
			blockHitResult = entityHitResult;
		}
		return blockHitResult;
	}

	@Override
	public void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		BlockState hitState = this.world.getBlockState(blockHitResult.getBlockPos());
		if (!hitState.getFluidState().isEmpty()) {
			if (hitState.getFluidState().isIn(FluidTags.WATER)) {
				if (
					blockHitResult.getSide() == Direction.UP &&
					this.getVelocity().horizontalLengthSquared() >= BigGlobeMath.squareD(this.getVelocity().y) * 3.0D
				) {
					this.bounce(blockHitResult, true);
				}
				//else go through surface
			}
			else if (hitState.getFluidState().isIn(FluidTags.LAVA)) {
				if (this.world instanceof ServerWorld world) {
					world.spawnParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 16, 0.0D, 0.0D, 0.0D, 0.0D);
					world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				}
				this.discard();
			}
		}
		else if (
			blockHitResult.getSide() == Direction.UP &&
			this.getVelocity().lengthSquared() < 0.125D * 0.125D
		) {
			this.placeRock(blockHitResult);
		}
		else {
			if (hitState.getMaterial() == Material.GLASS) {
				this.world.breakBlock(blockHitResult.getBlockPos(), true, this);
				this.setVelocity(this.getVelocity().multiply(0.75D));
			}
			else {
				this.bounce(blockHitResult, false);
			}
		}
	}

	public void placeRock(BlockHitResult blockHitResult) {
		BlockPos placePos = blockHitResult.getBlockPos().up();
		SingleBlockFeature.place(this.world, placePos, BigGlobeBlocks.ROCK.getDefaultState(), SingleBlockFeature.IS_REPLACEABLE);
		BlockSoundGroup group = BlockSoundGroup.STONE;
		this.world.playSound(null, placePos, group.getBreakSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.8F);
		this.discard();
	}

	public void bounce(BlockHitResult blockHitResult, boolean water) {
		Vec3d velocity = this.getVelocity();
		Axis axis = blockHitResult.getSide().getAxis();
		this.setVelocity(
			(axis == Axis.X ? -0.25D : 0.75D) * velocity.x,
			(axis == Axis.Y ? -0.25D : 0.75D) * velocity.y,
			(axis == Axis.Z ? -0.25D : 0.75D) * velocity.z
		);
		this.velocityDirty = true;
		if (water) {
			this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
		}
		else {
			BlockSoundGroup group = BlockSoundGroup.STONE;
			this.world.playSound(null, this.getX(), this.getY(), this.getZ(), group.getHitSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.5F);
		}
	}

	@Override
	public float getGravity() {
		return 0.05F;
	}
}