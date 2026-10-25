package builderb0y.bigglobe.items;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;

public class VoidmetalArmorMaterial {

	public static final Holder<ArmorMaterial> INSTANCE;

	static {
		EnumMap<ArmorItem.Type, Integer> defence = new EnumMap<>(ArmorItem.Type.class);
		defence.put(ArmorItem.Type.BOOTS, 3);
		defence.put(ArmorItem.Type.LEGGINGS, 6);
		defence.put(ArmorItem.Type.CHESTPLATE, 8);
		defence.put(ArmorItem.Type.HELMET, 3);
		defence.put(ArmorItem.Type.BODY, 11);
		INSTANCE = Registry.registerForHolder(
			BuiltInRegistries.ARMOR_MATERIAL,
			BigGlobeMod.modID("voidmetal"),
			new ArmorMaterial(
				defence,
				15,
				BigGlobeSoundEvents.ITEM_ARMOR_EQUIP_VOIDMETAL,
				() -> Ingredient.of(BigGlobeItems.VOIDMETAL_INGOT),
				List.of(new ArmorMaterial.Layer(BigGlobeMod.modID("voidmetal"))),
				3.0F,
				0.0F
			)
		);
	}
}
