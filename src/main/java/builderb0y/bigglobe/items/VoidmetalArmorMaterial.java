package builderb0y.bigglobe.items;

import java.util.Collections;
import java.util.EnumMap;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.versions.RegistryVersions;

public class VoidmetalArmorMaterial {

	#if MC_VERSION >= MC_1_20_5
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
				return "voidmetal";
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