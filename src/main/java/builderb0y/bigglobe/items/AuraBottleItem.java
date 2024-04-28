package builderb0y.bigglobe.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import builderb0y.bigglobe.blocks.CloudColor;

public class AuraBottleItem extends Item {

	public final CloudColor color;

	public AuraBottleItem(Settings settings, CloudColor color) {
		super(settings);
		this.color = color;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}