package builderb0y.bigglobe.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

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
import builderb0y.bigglobe.versions.ItemStackVersions;

#if MC_VERSION >= MC_1_21_0
	import net.minecraft.item.tooltip.TooltipType;
#elif MC_VERSION >= MC_1_20_5
	import net.minecraft.client.item.TooltipType;
#else
	import net.minecraft.client.item.TooltipContext;
#endif

public class BallOfStringItem extends Item
	#if MC_VERSION < MC_1_20_5
	implements DynamicMaxDamageItem
	#endif
{

	public static final String MAX_DAMAGE_KEY = "MaxDamage";

	public BallOfStringItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ItemStack stack = context.getStack();
		int damage = ItemStackVersions.getDamage(stack);
		int maxDamage = ItemStackVersions.getMaxDamage(stack);
		if (damage < maxDamage) {
			ItemStackVersions.setDamage(stack, damage + 1);
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

	#if MC_VERSION < MC_1_20_5

		@Override
		public int bigglobe_getMaxDamage(ItemStack stack) {
			NbtCompound nbt = stack.getNbt();
			return nbt != null ? nbt.getInt(MAX_DAMAGE_KEY) : 0;
		}

		@Override
		public void bigglobe_setMaxDamage(ItemStack stack, int maxDamage) {
			stack.getOrCreateNbt().putInt(MAX_DAMAGE_KEY, maxDamage);
		}

		@Override
		public boolean isDamageable() {
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
	#endif

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	#if MC_VERSION >= MC_1_20_5
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
	#else
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		super.appendTooltip(stack, world, tooltip, context);
	#endif

		int damage = ItemStackVersions.getDamage(stack);
		int maxDamage = ItemStackVersions.getMaxDamage(stack);
		tooltip.add(Text.translatable("tooltip." + BigGlobeMod.MODID + ".ball_of_string.remaining", maxDamage - damage, maxDamage));
	}

	public static void addString(ItemStack stack, int string) {
		int damage = ItemStackVersions.getDamage(stack);
		int maxDamage = ItemStackVersions.getMaxDamage(stack);
		damage -= string;
		if (damage < 0) {
			maxDamage -= damage;
			damage = 0;
		}
		ItemStackVersions.setDamage(stack, damage);
		ItemStackVersions.setMaxDamage(stack, maxDamage);
	}
}