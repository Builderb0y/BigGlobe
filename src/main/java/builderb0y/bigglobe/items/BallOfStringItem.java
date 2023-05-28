package builderb0y.bigglobe.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.StringEntity;

public class BallOfStringItem extends Item implements DynamicMaxDamageItem {

	public static final String MAX_DAMAGE_KEY = "MaxDamage";

	public BallOfStringItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		NbtCompound nbt = context.getStack().getOrCreateNbt();
		int damage = nbt.getInt(ItemStack.DAMAGE_KEY);
		int maxDamage = nbt.getInt(MAX_DAMAGE_KEY);
		if (damage < maxDamage) {
			nbt.putInt(ItemStack.DAMAGE_KEY, damage + 1);
			Vec3d pos = context.getHitPos();
			StringEntity entity = new StringEntity(BigGlobeEntityTypes.STRING, context.getWorld(), pos.x, pos.y, pos.z);
			entity.setNextEntity(context.getPlayer());
			context.getWorld().spawnEntity(entity);
			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		return super.useOnEntity(stack, user, entity, hand);
	}

	@Override
	public int bigglobe_getMaxDamage(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null ? nbt.getInt(MAX_DAMAGE_KEY) : 0;
	}

	@Override
	public boolean isDamageable() {
		return true;
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		//copy-paste of the vanilla method, but redirecting field accesses to my getter method.
		return Math.round(13.0F - ((float)(stack.getDamage())) * 13.0F / ((float)(this.bigglobe_getMaxDamage(stack))));
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		//copy-paste of the vanilla method, but redirecting field accesses to my getter method.
		float f = Math.max(0.0F, (((float)(this.bigglobe_getMaxDamage(stack))) - ((float)(stack.getDamage()))) / ((float)(this.bigglobe_getMaxDamage(stack))));
		return MathHelper.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		super.appendTooltip(stack, world, tooltip, context);

		NbtCompound nbt = stack.getOrCreateNbt();
		int damage = nbt.getInt(ItemStack.DAMAGE_KEY);
		int maxDamage = nbt.getInt(MAX_DAMAGE_KEY);
		tooltip.add(Text.translatable("tooltip." + BigGlobeMod.MODID + ".ball_of_string.remaining", maxDamage - damage));
	}

	public static void addString(ItemStack stack, int string) {
		NbtCompound nbt = stack.getOrCreateNbt();
		int damage = nbt.getInt(ItemStack.DAMAGE_KEY);
		int maxDamage = nbt.getInt(MAX_DAMAGE_KEY);
		damage -= string;
		if (damage < 0) {
			maxDamage -= damage;
			damage = 0;
		}
		nbt.putInt(ItemStack.DAMAGE_KEY, damage);
		nbt.putInt(MAX_DAMAGE_KEY, maxDamage);
	}
}