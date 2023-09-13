package builderb0y.bigglobe.scripting.wrappers;

import java.util.Arrays;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;

public class CraftingGrid extends ArrayWrapper<ItemStack> {

	public final int width;
	public final int height;
	public final boolean mutable;

	public CraftingGrid(Stream<ItemStack> stream, int width, int height, boolean mutable) {
		super(
			stream
			.map(ItemStack::copy) //defend against scripts modifying immutable crafting grids.
			.toArray(ItemStack[]::new)
		);
		this.width = width;
		this.height = height;
		this.mutable = mutable;
	}

	public ItemStack get(int x, int y) {
		ItemStack stack = this.get(y * this.width + x);
		return stack != null ? stack : ItemStack.EMPTY;
	}

	@Override
	public ItemStack set(int index, ItemStack stack) {
		if (this.mutable) {
			ItemStack old = this.elements[index];
			this.elements[index] = stack != null ? stack : ItemStack.EMPTY;
			return old;
		}
		else {
			throw new UnsupportedOperationException("Crafting grid cannot be modified.");
		}
	}

	public ItemStack set(int x, int y, ItemStack stack) {
		return this.set(y * this.width + x, stack);
	}

	@Override
	public String toString() {
		return "CraftingGrid: { size: " + this.width + 'x' + this.height + ", items: " + Arrays.toString(this.elements) + ", " + (this.mutable ? "mutable" : "immutable") + " }";
	}
}