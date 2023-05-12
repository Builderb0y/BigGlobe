package builderb0y.bigglobe.entities;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
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

	@Override
	public void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		if (
			blockHitResult.getSide() == Direction.UP &&
			this.getVelocity().lengthSquared() < 0.125D * 0.125D
		) {
			BlockPos placePos = blockHitResult.getBlockPos().up();
			BlockState existingState = this.world.getBlockState(placePos);
			if (existingState.getMaterial().isReplaceable()) {
				BlockState toPlace = BigGlobeBlocks.ROCK.getDefaultState().with(Properties.WATERLOGGED, existingState.getFluidState().isEqualAndStill(Fluids.WATER));
				if (toPlace.canPlaceAt(this.world, placePos)) {
					this.world.setBlockState(placePos, toPlace, Block.NOTIFY_ALL);
				}
			}
			BlockSoundGroup group = BlockSoundGroup.STONE;
			this.world.playSound(null, placePos, group.getBreakSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.8F);
			this.discard();
		}
		else {
			Vec3d velocity = this.getVelocity();
			Axis axis = blockHitResult.getSide().getAxis();
			this.setVelocity(
				(axis == Axis.X ? -0.25D : 0.75D) * velocity.x,
				(axis == Axis.Y ? -0.25D : 0.75D) * velocity.y,
				(axis == Axis.Z ? -0.25D : 0.75D) * velocity.z
			);
			this.velocityDirty = true;
			BlockSoundGroup group = BlockSoundGroup.STONE;
			this.world.playSound(null, this.getX(), this.getY(), this.getZ(), group.getHitSound(), SoundCategory.BLOCKS, group.getVolume() * 0.5F + 0.5F, group.getPitch() * 0.5F);
		}
	}

	@Override
	public float getGravity() {
		return 0.05F;
	}
}