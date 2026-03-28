package builderb0y.bigglobe.items;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.entities.StringEntity;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class BallOfStringItem extends Item {

	public static final String MAX_DAMAGE_KEY = "MaxDamage";

	public BallOfStringItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		int damage = ItemStackVersions.getDamage(stack);
		int maxDamage = ItemStackVersions.getMaxDamage(stack);
		if (damage < maxDamage) {
			ItemStackVersions.setDamage(stack, damage + 1);
			Vec3 pos = context.getClickLocation();
			StringEntity entity = new StringEntity(BigGlobeEntityTypes.STRING, context.getLevel(), pos.x, pos.y, pos.z);
			entity.setNextEntity(context.getPlayer());
			context.getLevel().addFreshEntity(entity);
			return InteractionResult.SUCCESS;
		}
		else {
			return InteractionResult.PASS;
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override

	@Deprecated
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
		super.appendHoverText(stack, context, displayComponent, tooltip, type);

		int damage = ItemStackVersions.getDamage(stack);
		int maxDamage = ItemStackVersions.getMaxDamage(stack);
		tooltip.accept(Component.translatable("tooltip." + BigGlobeMod.MODID + ".ball_of_string.remaining", maxDamage - damage, maxDamage));
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