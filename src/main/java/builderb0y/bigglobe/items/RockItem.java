package builderb0y.bigglobe.items;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.RockEntity;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;

public class RockItem extends BlockItem implements SlingshotAmmunition {

	public RockItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	/** mostly copy-pasted from {@link SnowballItem}. */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), BigGlobeSoundEvents.ENTITY_ROCK_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClient) {
			RockEntity rockEntity = new RockEntity(BigGlobeEntityTypes.ROCK, user, world);
			rockEntity.setItem(stack);
			rockEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 0.75F, 8.0F);
			world.spawnEntity(rockEntity);
		}
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		if (!user.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		return TypedActionResult.success(stack, true);
	}

	@Override
	public ProjectileEntity createProjectile(World world, LivingEntity user, ItemStack stack, ItemStack slingshot) {
		RockEntity rockEntity = new RockEntity(BigGlobeEntityTypes.ROCK, user, world);
		rockEntity.setItem(stack);
		return rockEntity;
	}
}