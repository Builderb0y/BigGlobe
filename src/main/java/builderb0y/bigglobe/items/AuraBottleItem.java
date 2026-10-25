package builderb0y.bigglobe.items;

import builderb0y.bigglobe.blocks.CloudColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AuraBottleItem extends Item {

	public final CloudColor color;

	public AuraBottleItem(Properties settings, CloudColor color) {
		super(settings);
		this.color = color;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}