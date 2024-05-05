package builderb0y.bigglobe.items;

import java.util.Collections;
import java.util.EnumMap;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.versions.RegistryVersions;

public class VoidmetalArmorMaterial {

	public static final RegistryEntry<ArmorMaterial> INSTANCE;
	static {
		EnumMap<ArmorItem.Type, Integer> defence = new EnumMap<>(ArmorItem.Type.class);
		defence.put(ArmorItem.Type.BOOTS, 3);
		defence.put(ArmorItem.Type.LEGGINGS, 6);
		defence.put(ArmorItem.Type.CHESTPLATE, 8);
		defence.put(ArmorItem.Type.HELMET, 3);
		defence.put(ArmorItem.Type.BODY, 11);
		INSTANCE = Registry.registerReference(
			RegistryVersions.armorMaterial(),
			BigGlobeMod.modID("voidmetal"),
			new ArmorMaterial(
				defence,
				15,
				BigGlobeSoundEvents.ITEM_ARMOR_EQUIP_VOIDMETAL,
				() -> Ingredient.ofItems(BigGlobeItems.VOIDMETAL_INGOT),
				Collections.singletonList(new ArmorMaterial.Layer(BigGlobeMod.modID("voidmetal"))),
				3.0F,
				0.0F
			)
		);
	}
}