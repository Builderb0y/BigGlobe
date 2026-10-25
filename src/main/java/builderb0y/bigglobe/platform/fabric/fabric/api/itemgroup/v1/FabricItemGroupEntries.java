package builderb0y.bigglobe.platform.fabric.fabric.api.itemgroup.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class FabricItemGroupEntries {

	private final List<ItemStack> entries = new ArrayList<>();

	public void accept(ItemStack stack) {
		this.entries.add(stack);
	}

	public void accept(ItemLike item) {
		this.accept(new ItemStack(item));
	}

	public void add(ItemStack stack) {
		this.accept(stack);
	}

	public void add(ItemLike item) {
		this.accept(item);
	}

	public void addAfter(ItemLike after, ItemLike... items) {
		for (ItemLike item : items) {
			this.entries.add(new ItemStack(item));
		}
	}

	public void addAfter(ItemLike after, ItemStack... items) {
		this.entries.addAll(Arrays.asList(items));
	}

	public void addBefore(ItemLike before, ItemLike... items) {
		for (ItemLike item : items) {
			this.entries.add(new ItemStack(item));
		}
	}

	public void addBefore(ItemLike before, ItemStack... items) {
		this.entries.addAll(Arrays.asList(items));
	}

	public List<ItemStack> getEntries() {
		return this.entries;
	}
}
