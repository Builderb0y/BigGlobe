package builderb0y.bigglobe.items;

import java.util.EnumMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.itemdefs.BigGlobeItemTags;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;

public class VoidmetalArmorMaterial {

	public static final ResourceKey<EquipmentAsset> MODEL = ResourceKey.create(
		EquipmentAssets.ROOT_ID,
		BigGlobeMod.modID("voidmetal")
	);
	public static final ArmorMaterial INSTANCE;

	static {
		EnumMap<ArmorType, Integer> defence = new EnumMap<>(ArmorType.class);
		defence.put(ArmorType.BOOTS, 3);
		defence.put(ArmorType.LEGGINGS, 6);
		defence.put(ArmorType.CHESTPLATE, 8);
		defence.put(ArmorType.HELMET, 3);
		defence.put(ArmorType.BODY, 11);
		INSTANCE = new ArmorMaterial(
			37,
			defence,
			15,
			BigGlobeSoundEvents.ITEM_ARMOR_EQUIP_VOIDMETAL,
			3.0F,
			0.0F,
			BigGlobeItemTags.REPAIRS_VOIDMETAL_ARMOR,
			MODEL
		);
	}
}