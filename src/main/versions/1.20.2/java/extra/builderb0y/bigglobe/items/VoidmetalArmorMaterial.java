package builderb0y.bigglobe.items;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;

import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;

public class VoidmetalArmorMaterial {

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
}