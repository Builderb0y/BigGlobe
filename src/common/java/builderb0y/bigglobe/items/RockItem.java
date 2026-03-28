package builderb0y.bigglobe.items;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.RockEntity;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.versions.ActionResultVersions;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class RockItem extends BlockItem implements SlingshotAmmunition {

	public RockItem(Block block, Item.Properties settings) {
		super(block, settings);
	}

	/**
	mostly copy-pasted from {@link SnowballItem}.
	*/
	@Override
	public InteractionResult

	use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), BigGlobeSoundEvents.ENTITY_ROCK_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClientSide()) {
			RockEntity rockEntity = new RockEntity(BigGlobeEntityTypes.ROCK, user, world);
			rockEntity.setItem(stack);
			rockEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0F, 0.75F, 8.0F);
			world.addFreshEntity(rockEntity);
		}
		user.awardStat(Stats.ITEM_USED.get(this));
		if (!user.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return ActionResultVersions.typedSuccess(stack);
	}

	@Override
	public Projectile createProjectile(Level world, LivingEntity user, ItemStack stack, ItemStack slingshot) {
		RockEntity rockEntity = new RockEntity(BigGlobeEntityTypes.ROCK, user, world);
		rockEntity.setItem(stack);
		return rockEntity;
	}

	@Override
	public Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction) {
		RockEntity rockEntity = new RockEntity(BigGlobeEntityTypes.ROCK, pos.y(), pos.y(), pos.z(), world);
		rockEntity.setItem(stack);
		return rockEntity;
	}
}