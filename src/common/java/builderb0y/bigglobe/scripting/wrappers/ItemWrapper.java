package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import builderb0y.bigglobe.scripting.wrappers.tags.ItemTag;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ItemWrapper {

	public static final TypeInfo TYPE = type(Item.class);

	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ItemWrapper.class, "getItem", String.class, Item.class);

	public static Item getItem(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return getItem(id, flags);
	}

	public static Item getItem(String id, int flags) {
		if (id == null) return null;
		try {
			Identifier identifier = IdentifierVersions.create(id);
			if (BuiltInRegistries.ITEM.containsKey(identifier)) {
				return BuiltInRegistries.ITEM.getValue(identifier);
			}
			else {
				throw new IllegalArgumentException("Unknown item: " + identifier);
			}
		}
		catch (RuntimeException exception) {
			if ((flags & AbstractConstantFactory.NULLABLE) != 0) return null;
			else throw exception;
		}
	}

	@SuppressWarnings("deprecation")
	public static String id(Item item) {
		return UnregisteredObjectException.getID(item.builtInRegistryHolder()).toString();
	}

	public static boolean isIn(Item item, ItemTag tag) {
		return tag.contains(item);
	}

	public static ItemStack getDefaultStack(Item item) {
		return item.getDefaultInstance();
	}
}