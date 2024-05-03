package builderb0y.bigglobe.items;

import net.minecraft.item.ItemStack;

public interface DynamicMaxDamageItem {

	public abstract int bigglobe_getMaxDamage(ItemStack stack);

	public abstract void bigglobe_setMaxDamage(ItemStack stack, int maxDamage);
}