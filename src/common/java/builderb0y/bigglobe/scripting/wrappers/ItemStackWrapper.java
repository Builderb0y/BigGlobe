package builderb0y.bigglobe.scripting.wrappers;

import builderb0y.bigglobe.scripting.wrappers.tags.ItemTag;
import builderb0y.bigglobe.scripting.wrappers.tags.TagParser;
import builderb0y.bigglobe.versions.ItemStackVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.BuiltinScriptEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

	public static ItemStack create(Item item, CompoundTag nbt) {
		Tag oldID = nbt.put("id", StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
		try {
			return create(nbt);
		}
		finally {
			if (oldID != null) nbt.put("id", oldID);
		}
	}

	public static ItemStack create(Item item, int count, CompoundTag nbt) {
		Tag oldID = nbt.put("id", StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
		try {
			Tag oldCount = nbt.put("count", IntTag.valueOf(count));
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

	public static ItemStack create(CompoundTag nbt) {
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
		return stack.getMaxStackSize();
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
		return stack.isDamageableItem();
	}

	public static int damage(ItemStack stack) {
		return stack.getDamageValue();
	}

	//////////////////////////////// nbt ////////////////////////////////

	public static CompoundTag nbt(ItemStack stack) {
		return ItemStackVersions.toNbt(stack);
	}

	//////////////////////////////// other ////////////////////////////////

	public static boolean isIn(ItemStack stack, ItemTag tag) {
		return tag.list.contains(stack.typeHolder());
	}
}