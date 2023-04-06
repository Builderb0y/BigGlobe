package builderb0y.bigglobe.brewing;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.items.BigGlobeItems;

public class BigGlobeBrewing {

	static { BigGlobeMod.LOGGER.debug("Registering potions..."); }

	public static final Potion
		WITHER        = register("wither",        new Potion("wither",        new StatusEffectInstance(StatusEffects.WITHER,  600, 0))),
		LONG_WITHER   = register("long_wither",   new Potion("long_wither",   new StatusEffectInstance(StatusEffects.WITHER, 1200, 0))),
		STRONG_WITHER = register("strong_wither", new Potion("strong_wither", new StatusEffectInstance(StatusEffects.WITHER,  300, 1)));

	static { BigGlobeMod.LOGGER.debug("Done registering potions."); }

	public static void init() {
		BigGlobeMod.LOGGER.debug("Registering potion recipes...");
		BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, BigGlobeItems.ASH, WITHER);
		BrewingRecipeRegistry.registerPotionRecipe(WITHER, Items.REDSTONE, LONG_WITHER);
		BrewingRecipeRegistry.registerPotionRecipe(WITHER, Items.GLOWSTONE_DUST, STRONG_WITHER);
		BigGlobeMod.LOGGER.debug("Done registering potion recipes.");
	}

	public static Potion register(String name, Potion potion) {
		return Registry.register(Registries.POTION, BigGlobeMod.modID(name), potion);
	}
}