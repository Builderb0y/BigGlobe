package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ItemStackWrapper {

	public static final TypeInfo TYPE = type(ItemStack.class);
	public static final ItemStack EMPTY = ItemStack.EMPTY;

	//////////////////////////////// creating stacks ////////////////////////////////

	public static ItemStack create(Item item) {
		return new ItemStack(item);
	}

	public static ItemStack create(Item item, int count) {
		return new ItemStack(item, count);
	}

	public static ItemStack create(Item item, NbtCompound nbt) {
		ItemStack stack = new ItemStack(item);
		stack.setNbt(nbt);
		return stack;
	}

	public static ItemStack create(Item item, int count, NbtCompound nbt) {
		ItemStack stack = new ItemStack(item, count);
		stack.setNbt(nbt);
		return stack;
	}

	public static ItemStack create(NbtCompound nbt) {
		return ItemStack.fromNbt(nbt);
	}

	public static Item item(ItemStack stack) {
		return stack.getItem();
	}

	public static ItemStack copy(ItemStack stack) {
		return stack.copy();
	}

	//////////////////////////////// count ////////////////////////////////

	public static boolean empty(ItemStack stack) {
		return stack.isEmpty();
	}

	public static int maxCount(ItemStack stack) {
		return stack.getMaxCount();
	}

	public static boolean stackable(ItemStack stack) {
		return stack.isStackable();
	}

	public static int count(ItemStack stack) {
		return stack.getCount();
	}

	public static void count(ItemStack stack, int count) {
		stack.setCount(count);
	}

	//////////////////////////////// damage ////////////////////////////////

	public static int maxDamage(ItemStack stack) {
		return stack.getMaxDamage();
	}

	public static boolean damageable(ItemStack stack) {
		return stack.isDamageable();
	}

	public static int damage(ItemStack stack) {
		return stack.getDamage();
	}

	public static void damage(ItemStack stack, int damage) {
		stack.setDamage(damage);
	}

	//////////////////////////////// nbt ////////////////////////////////

	public static NbtCompound nbt(ItemStack stack) {
		return stack.getNbt();
	}

	public static void nbt(ItemStack stack, NbtCompound nbt) {
		stack.setNbt(nbt);
	}
}