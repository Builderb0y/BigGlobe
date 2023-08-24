package builderb0y.bigglobe.items;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.function.Predicate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.EntityVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

/**
mostly a copy-paste of {@link BowItem} but edited to work with rocks,
which are not an instance of {@link ArrowItem}.
*/
public class SlingshotItem extends RangedWeaponItem implements Vanishable {

	public static final TagKey<Item> AMMUNITION = TagKey.of(RegistryKeyVersions.item(), BigGlobeMod.modID("slingshot_ammunition"));
	public static final Predicate<ItemStack> AMMUNITION_PREDICATE = (ItemStack stack) -> stack.isIn(AMMUNITION);

	public SlingshotItem(Settings settings) {
		super(settings);
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		float progress;
		if (!(user instanceof PlayerEntity playerEntity)) {
			return;
		}
		boolean creative = playerEntity.getAbilities().creativeMode;
		ItemStack ammunitionStack = EntityVersions.getAmmunition(playerEntity, stack);
		if ((ammunitionStack.isEmpty() || ammunitionStack.getItem() == Items.ARROW) && !creative) {
			return;
		}
		if (ammunitionStack.isEmpty() || ammunitionStack.getItem() == Items.ARROW) {
			ammunitionStack = new ItemStack(BigGlobeItems.ROCK);
		}
		if ((progress = BowItem.getPullProgress(this.getMaxUseTime(stack) - remainingUseTicks)) < 0.1F) {
			return;
		}
		boolean creativeRock = creative && ammunitionStack.isOf(BigGlobeItems.ROCK);
		if (!world.isClient) {
			SlingshotAmmunition arrowItem = ammunitionStack.getItem() instanceof SlingshotAmmunition ammo ? ammo : (SlingshotAmmunition)(BigGlobeItems.ROCK);
			ProjectileEntity projectile = arrowItem.createProjectile(world, user, ammunitionStack, stack);
			projectile.setVelocity(playerEntity, playerEntity.getPitch(), playerEntity.getYaw(), 0.0F, progress * 1.5F, 1.0F);
			stack.damage(1, playerEntity, p -> p.sendToolBreakStatus(p.getActiveHand()));
			world.spawnEntity(projectile);
		}
		world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + progress * 0.5F);
		if (!creativeRock && !playerEntity.getAbilities().creativeMode) {
			ammunitionStack.decrement(1);
			if (ammunitionStack.isEmpty()) {
				playerEntity.getInventory().removeOne(ammunitionStack);
			}
		}
		playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 72000;
	}

	public static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
	/** this is a client-side class, so we need to be extra careful to not load it on the server. */
	public static final Class<?> PLAYER_ENTITY_RENDERER = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? getPlayerEntityRenderer() : null;

	@Environment(EnvType.CLIENT)
	public static Class<?> getPlayerEntityRenderer() {
		return PlayerEntityRenderer.class;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
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
		return STACK_WALKER.walk(stream -> stream.map(StackFrame::getDeclaringClass).skip(3).findFirst()).orElse(null) == PLAYER_ENTITY_RENDERER ? UseAction.TOOT_HORN : UseAction.NONE;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		boolean hasAmmunition = !EntityVersions.getAmmunition(user, itemStack).isEmpty();
		if (user.getAbilities().creativeMode || hasAmmunition) {
			user.setCurrentHand(hand);
			return TypedActionResult.consume(itemStack);
		}
		return TypedActionResult.fail(itemStack);
	}

	@Override
	public Predicate<ItemStack> getProjectiles() {
		return AMMUNITION_PREDICATE;
	}

	@Override
	public int getRange() {
		return 15;
	}
}