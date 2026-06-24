package builderb0y.bigglobe.items;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.function.Predicate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import builderb0y.bigglobe.itemdefs.BigGlobeItemTags;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;
import builderb0y.bigglobe.versions.ActionResultVersions;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.ItemStackVersions;

/**
mostly a copy-paste of {@link BowItem} but edited to work with rocks,
which are not an instance of {@link ArrowItem}.
*/
public class SlingshotItem extends ProjectileWeaponItem {

	public static final Predicate<ItemStack> AMMUNITION_PREDICATE = (ItemStack stack) -> stack.is(BigGlobeItemTags.SLINGSHOT_AMMUNITION);

	public SlingshotItem(Properties settings) {
		super(settings);
	}

	@Override
	public boolean

	releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		float progress;
		if (!(user instanceof Player playerEntity)) {
			return false;
		}
		boolean creative = playerEntity.getAbilities().instabuild;
		ItemStack ammunitionStack = EntityVersions.getAmmunition(playerEntity, stack);
		if ((ammunitionStack.isEmpty() || ammunitionStack.getItem() == Items.ARROW) && !creative) {
			return false;
		}
		if (ammunitionStack.isEmpty() || ammunitionStack.getItem() == Items.ARROW) {
			ammunitionStack = new ItemStack(BigGlobeItems.ROCK);
		}
		if ((progress = BowItem.getPowerForTime(this.getUseDuration(stack, user) - remainingUseTicks)) < 0.1F) {
			return false;
		}
		boolean creativeRock = creative && ammunitionStack.is(BigGlobeItems.ROCK);
		if (!world.isClientSide()) {
			SlingshotAmmunition arrowItem = ammunitionStack.getItem() instanceof SlingshotAmmunition ammo ? ammo : (SlingshotAmmunition)(BigGlobeItems.ROCK);
			Projectile projectile = arrowItem.createProjectile(world, user, ammunitionStack, stack);
			projectile.shootFromRotation(playerEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, progress * 1.5F, 1.0F);
			ItemStackVersions.damage(stack, playerEntity, playerEntity.getUsedItemHand());
			world.addFreshEntity(projectile);
		}
		world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + progress * 0.5F);
		if (!creativeRock && !playerEntity.getAbilities().instabuild) {
			ammunitionStack.shrink(1);
			if (ammunitionStack.isEmpty()) {
				playerEntity.getInventory().removeItem(ammunitionStack);
			}
		}
		playerEntity.awardStat(Stats.ITEM_USED.get(this));

		return true;
	}

	@Override
	public void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target) {}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
	/**
	this is a client-side class, so we need to be extra careful to not load it on the server.
	*/
	public static final Class<?> PLAYER_ENTITY_RENDERER = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? getPlayerEntityRenderer() : null;

	@Environment(EnvType.CLIENT)
	public static Class<?> getPlayerEntityRenderer() {
		return AvatarRenderer.class;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		/**
		welcome to hacky code 101.
		the first person renderer needs to apply no offsets, rotations,
		or other transformations, as if we had returned {@link UseAction#NONE}.
		however, the 3rd person renderer needs to hold their arm out in front of them.
		the only UseAction which does this is {@link UseAction#TOOT_HORN}.
		so what do we do? check the caller class.
		or technically the caller's caller, since the first
		caller is always {@link ItemStack#getUseAction()}.
		*/
		return STACK_WALKER.walk(stream -> stream.map(StackFrame::getDeclaringClass).skip(3).findFirst()).orElse(null) == PLAYER_ENTITY_RENDERER ? ItemUseAnimation.TOOT_HORN : ItemUseAnimation.NONE;
	}

	@Override
	public InteractionResult

	use(Level world, Player user, InteractionHand hand) {
		ItemStack itemStack = user.getItemInHand(hand);
		boolean hasAmmunition = !EntityVersions.getAmmunition(user, itemStack).isEmpty();
		if (user.getAbilities().instabuild || hasAmmunition) {
			user.startUsingItem(hand);
			return ActionResultVersions.typedConsume(itemStack);
		}
		return ActionResultVersions.typedFail(itemStack);
	}

	@Override
	public Predicate<ItemStack> getAllSupportedProjectiles() {
		return AMMUNITION_PREDICATE;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 15;
	}
}