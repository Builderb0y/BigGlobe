package builderb0y.bigglobe.items;

import java.util.EnumMap;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;

public class VoidmetalArmorMaterial {

	public static final RegistryKey<EquipmentAsset> MODEL = RegistryKey.of(
		EquipmentAssetKeys.REGISTRY_KEY,
		BigGlobeMod.modID("voidmetal")
	);
	public static final ArmorMaterial INSTANCE;
	static {
		EnumMap<EquipmentType, Integer> defence = new EnumMap<>(EquipmentType.class);
		defence.put(EquipmentType.BOOTS,      3);
		defence.put(EquipmentType.LEGGINGS,   6);
		defence.put(EquipmentType.CHESTPLATE, 8);
		defence.put(EquipmentType.HELMET,     3);
		defence.put(EquipmentType.BODY,      11);
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