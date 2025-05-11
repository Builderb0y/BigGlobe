package builderb0y.bigglobe.scripting.wrappers;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;

import builderb0y.bigglobe.scripting.wrappers.tags.ItemTag;
import builderb0y.bigglobe.scripting.wrappers.tags.TagParser;
import builderb0y.bigglobe.versions.ItemStackVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ItemStackWrapper {

	public static final TypeInfo TYPE = type(ItemStack.class);
	public static final ItemStack EMPTY = ItemStack.EMPTY;
	public static final TagParser TAG_PARSER = new TagParser("ItemTag", ItemTag.class, "ItemStack", MethodInfo.inCaller("isIn"));

	//////////////////////////////// creating stacks ////////////////////////////////

	public static ItemStack create(Item item) {
		return new ItemStack(item);
	}

	public static ItemStack create(Item item, int count) {
		return new ItemStack(item, count);
	}

	public static ItemStack create(Item item, NbtCompound nbt) {
		NbtElement oldID = nbt.put("id", NbtString.of(Registries.ITEM.getId(item).toString()));
		try {
			return create(nbt);
		}
		finally {
			if (oldID != null) nbt.put("id", oldID);
		}
	}

	public static ItemStack create(Item item, int count, NbtCompound nbt) {
		NbtElement oldID = nbt.put("id", NbtString.of(Registries.ITEM.getId(item).toString()));
		try {
			NbtElement oldCount = nbt.put("count", NbtInt.of(count));
			try {
				return create(nbt);
			}
			finally {
				if (oldCount != null) nbt.put("count", oldCount);
			}
		}
		finally {
			if (oldID != null) nbt.put("id", oldID);
		}
	}

	public static ItemStack create(NbtCompound nbt) {
		ItemStack stack = ItemStackVersions.fromNbt(nbt);
		if (stack.isEmpty()) {
			BuiltinScriptEnvironment.PRINTER.println("A script attempted to create an ItemStack from invalid NBT data: " + nbt);
		}
		return stack;
	}

	public static Item item(ItemStack stack) {
		return stack.getItem();
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

	//////////////////////////////// nbt ////////////////////////////////

	public static NbtCompound nbt(ItemStack stack) {
		return ItemStackVersions.toNbt(stack);
	}

	//////////////////////////////// other ////////////////////////////////

	public static boolean isIn(ItemStack stack, ItemTag tag) {
		return tag.list.contains(stack.getRegistryEntry());
	}
}