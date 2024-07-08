package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ItemWrapper {

	public static final TypeInfo TYPE = type(Item.class);

	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(ItemWrapper.class, "getItem", String.class, Item.class);

	public static Item getItem(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getItem(id);
	}

	public static Item getItem(String id) {
		if (id == null) return null;
		Identifier identifier = IdentifierVersions.create(id);
		if (RegistryVersions.item().containsId(identifier)) {
			return RegistryVersions.item().get(identifier);
		}
		else {
			throw new IllegalArgumentException("Unknown item: " + identifier);
		}
	}

	@SuppressWarnings("deprecation")
	public static String id(Item item) {
		return UnregisteredObjectException.getID(item.getRegistryEntry()).toString();
	}

	@SuppressWarnings("deprecation")
	public static boolean isIn(Item item, ItemTagKey key) {
		return item.getRegistryEntry().isIn(key.key());
	}

	public static ItemStack getDefaultStack(Item item) {
		return item.getDefaultStack();
	}
}