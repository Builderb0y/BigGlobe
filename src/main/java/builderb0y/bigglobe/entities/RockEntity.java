package builderb0y.bigglobe.entities;

import java.util.function.Predicate;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
			this.getDamageSources().thrown(this, this.getOwner()),
			(float)(this.getVelocity().length() * 8.0D)
		);
		this.discard();
	}

	/**
	mostly a copy-paste of {@link ProjectileUtil#getCollision(Entity, Predicate)},
	but allowing collisions with fluids.
	*/
	@SuppressWarnings("unused")
	public HitResult bigglobe_getCollision() {
		EntityHitResult hitResult2;
		Vec3d vec3d3;
		Vec3d vec3d = this.getVelocity();
		World world = this.world;
		Vec3d vec3d2 = this.getPos();
		HitResult hitResult = world.raycast(
			new RaycastContext(
				vec3d2,
				vec3d3 = vec3d2.add(vec3d),
				ShapeType.COLLIDER,
				this.world.getBlockState(BlockPos.ofFloored(this.getPos())).getBlock() == Blocks.WATER
				? FluidHandling.NONE
				: FluidHandling.SOURCE_ONLY,
				this
			)
		);
		if (hitResult.getType() != HitResult.Type.MISS) {
			vec3d3 = hitResult.getPos();
		}
		if ((hitResult2 = ProjectileUtil.getEntityCollision(world, this, vec3d2, vec3d3, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0), this::canHit)) != null) {
			hitResult = hitResult2;
		}
		return hitResult;
	}

	@Override
	public void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (
			this.world.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.WATER
		) {
			if (
				blockHitResult.getSide() == Direction.UP &&
				this.getVelocity().lengthSquared() >= 0.125D * 0.125D
			) {
				this.bounce(blockHitResult, true);
			}
			//else go through surface
		}
		else if (
			blockHitResult.getSide() == Direction.UP &&
			this.getVelocity().lengthSquared() < 0.125D * 0.125D
		) {
			this.placeRock(blockHitResult);
		}
		else {
			this.bounce(blockHitResult, false);
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