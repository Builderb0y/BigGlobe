package builderb0y.bigglobe.items;

import java.util.Collections;
import java.util.EnumMap;

import net.minecraft.item.ArmorItem;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION >= MC_1_21_4
	import net.minecraft.item.equipment.EquipmentAsset;
	import net.minecraft.item.equipment.EquipmentAssetKeys;
#endif

#if MC_VERSION >= MC_1_21_2
	import net.minecraft.item.equipment.ArmorMaterial;
	import net.minecraft.item.equipment.EquipmentType;
#else
	import net.minecraft.item.ArmorMaterial;
#endif

public class VoidmetalArmorMaterial {

	#if MC_VERSION >= MC_1_21_4

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

	#elif MC_VERSION >= MC_1_21_2

		public static final Identifier MODEL = BigGlobeMod.modID("voidmetal");
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

	#elif MC_VERSION >= MC_1_20_5

		public static final RegistryEntry<ArmorMaterial> INSTANCE;
		static {
			EnumMap<ArmorItem.Type, Integer> defence = new EnumMap<>(ArmorItem.Type.class);
			defence.put(ArmorItem.Type.BOOTS, 3);
			defence.put(ArmorItem.Type.LEGGINGS, 6);
			defence.put(ArmorItem.Type.CHESTPLATE, 8);
			defence.put(ArmorItem.Type.HELMET, 3);
			defence.put(ArmorItem.Type.BODY, 11);
			INSTANCE = Registry.registerReference(
				Registries.ARMOR_MATERIAL,
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

	#else

		public static final ArmorMaterial INSTANCE = new ArmorMaterial() {

			@Override
			public int getDurability(ArmorItem.Type type) {
				return switch (type) {
					case HELMET     -> 11 * 37;
					case CHESTPLATE -> 16 * 37;
					case LEGGINGS   -> 15 * 37;
					case BOOTS      -> 13 * 37;
				};
			}

			@Override
			public int getProtection(ArmorItem.Type type) {
				return switch (type) {
					case HELMET     -> 3;
					case CHESTPLATE -> 8;
					case LEGGINGS   -> 6;
					case BOOTS      -> 3;
				};
			}

			@Override
			public int getEnchantability() {
				return 15;
			}

			@Override
			public SoundEvent getEquipSound() {
				return BigGlobeSoundEvents.ITEM_ARMOR_EQUIP_VOIDMETAL.value();
			}

			@Override
			public Ingredient getRepairIngredient() {
				return Ingredient.ofItems(BigGlobeItems.VOIDMETAL_INGOT);
			}

			@Override
			public String getName() {
				return "bigglobe_voidmetal";
			}

			@Override
			public float getToughness() {
				return 3.0F;
			}

			@Override
			public float getKnockbackResistance() {
				return 0.0F;
			}
		};

	#endif
}