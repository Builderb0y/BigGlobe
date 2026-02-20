package builderb0y.bigglobe.entities;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
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

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.RockBlock;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.versions.EntityVersions;

public class RockEntity extends ThrownItemEntity {

	public RockEntity(EntityType<? extends RockEntity> entityType, World world) {
		super(entityType, world);
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, double d, double e, double f, World world) {
		super(entityType, d, e, f, world #if MC_VERSION >= MC_1_21_2 , new ItemStack(BigGlobeItems.ROCK) #endif);
	}

	public RockEntity(EntityType<? extends RockEntity> entityType, LivingEntity livingEntity, World world) {
		super(entityType, livingEntity, world #if MC_VERSION >= MC_1_21_2 , new ItemStack(BigGlobeItems.ROCK) #endif);
	}

	@Override
	public Item getDefaultItem() {
		return BigGlobeItems.ROCK;
	}

	@Override
	public void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		if (EntityVersions.getWorld(this) instanceof ServerWorld serverWorld) {
			entityHitResult.getEntity().damage(
				#if MC_VERSION >= MC_1_21_2
					serverWorld,
				#endif
				this.getDamageSources().thrown(this, this.getOwner()),
				(float)(this.getVelocity().length() * 6.0D)
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
		World world = EntityVersions.getWorld(this);
		FluidState fluidState = world.getFluidState(BlockPos.ofFloored(this.getX(), this.getY() + 0.125D, this.getZ()));
		Vec3d velocity = this.getVelocity();
		Vec3d position = EntityVersions.getPos(this);
		return world.raycast(
			new RaycastContext(
				position,
				position.add(velocity),
				ShapeType.COLLIDER,
				fluidState.isEmpty()
				? FluidHandling.ANY
				: FluidHandling.NONE,
				this
			)
		);
	}

	public void tryBounceCollision(BlockHitResult blockHitResult) {
		BlockState hitState = EntityVersions.getWorld(this).getBlockState(blockHitResult.getBlockPos());
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
				if (EntityVersions.getWorld(this) instanceof ServerWorld world) {
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
			if (hitState.isIn(BigGlobeBlockTags.ROCK_BREAKABLE)) {
				EntityVersions.getWorld(this).breakBlock(blockHitResult.getBlockPos(), true, this);
				this.setVelocity(this.getVelocity().multiply(0.75D));
			}
			else {
				this.bounce(blockHitResult, false);
			}
		}
	}

	public void placeRock(BlockHitResult blockHitResult) {
		BlockPos placePos = blockHitResult.getBlockPos().up();
		BlockState existingState = EntityVersions.getWorld(this).getBlockState(placePos);
		int rocks;
		if (existingState.isOf(BigGlobeBlocks.ROCK) && (rocks = existingState.get(RockBlock.ROCKS)) < 6) {
			EntityVersions.getWorld(this).setBlockState(placePos, existingState.with(RockBlock.ROCKS, rocks + 1));
		}
		else {
			SingleBlockFeature.place(EntityVersions.getWorld(this), placePos, BigGlobeBlocks.ROCK.getDefaultState(), SingleBlockFeature.IS_REPLACEABLE);
		}
		BlockSoundGroup group = BlockSoundGroup.STONE;
		EntityVersions.getWorld(this).playSound(null, placePos, group.getBreakSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.8F);
		this.discard();
	}

	public void bounce(BlockHitResult blockHitResult, boolean water) {
		Vec3d velocity = this.getVelocity();
		Axis axis = blockHitResult.getSide().getAxis();
		this.setVelocity(
			(axis == Axis.X ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.x,
			(axis == Axis.Y ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.y,
			(axis == Axis.Z ? (water ? -0.5D : -0.25D) : (water ? 1.0D : 0.75D)) * velocity.z
		);
		this.velocityDirty = true;
		if (water) {
			EntityVersions.getWorld(this).playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
		}
		else {
			BlockSoundGroup group = BlockSoundGroup.STONE;
			EntityVersions.getWorld(this).playSound(null, this.getX(), this.getY(), this.getZ(), group.getHitSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.5F);
		}
	}

	@Override
	public #if MC_VERSION >= MC_1_20_5 double #else float #endif getGravity() {
		return 0.05F;
	}
}