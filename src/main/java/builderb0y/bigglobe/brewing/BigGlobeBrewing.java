package builderb0y.bigglobe.brewing;

import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeBrewing {

	static { BigGlobeMod.LOGGER.debug("Registering potions..."); }

	public static final #if MC_VERSION >= MC_1_20_5 RegistryEntry<Potion> #else Potion #endif
		WITHER        = register("wither",        new Potion("wither",        new StatusEffectInstance(StatusEffects.WITHER,  600, 0))),
		LONG_WITHER   = register("long_wither",   new Potion("long_wither",   new StatusEffectInstance(StatusEffects.WITHER, 1200, 0))),
		STRONG_WITHER = register("strong_wither", new Potion("strong_wither", new StatusEffectInstance(StatusEffects.WITHER,  300, 1)));

	static { BigGlobeMod.LOGGER.debug("Done registering potions."); }

	public static void init() {
		#if MC_VERSION >= MC_1_20_5
			FabricBrewingRecipeRegistryBuilder.BUILD.register((BrewingRecipeRegistry.Builder builder) -> {
				BigGlobeMod.LOGGER.debug("Registering potion recipes...");
				builder.registerPotionRecipe(Potions.AWKWARD, BigGlobeItems.ASH, WITHER);
				builder.registerPotionRecipe(WITHER, Items.REDSTONE, LONG_WITHER);
				builder.registerPotionRecipe(WITHER, Items.GLOWSTONE_DUST, STRONG_WITHER);
				builder.registerPotionRecipe(Potions.WATER, BigGlobeItems.CHORUS_SPORE, Potions.AWKWARD);
				BigGlobeMod.LOGGER.debug("Done registering potion recipes.");
			});
		#else
			BigGlobeMod.LOGGER.debug("Registering potion recipes...");
			BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, BigGlobeItems.ASH, WITHER);
			BrewingRecipeRegistry.registerPotionRecipe(WITHER, Items.REDSTONE, LONG_WITHER);
			BrewingRecipeRegistry.registerPotionRecipe(WITHER, Items.GLOWSTONE_DUST, STRONG_WITHER);
			BrewingRecipeRegistry.registerPotionRecipe(Potions.WATER, BigGlobeItems.CHORUS_SPORE, Potions.AWKWARD);
			BigGlobeMod.LOGGER.debug("Done registering potion recipes.");
		#endif
	}

	public static #if MC_VERSION >= MC_1_20_5 RegistryEntry<Potion> #else Potion #endif register(String name, Potion potion) {
		return Registry. #if MC_VERSION >= MC_1_20_5 registerReference #else register #endif (RegistryVersions.potion(), BigGlobeMod.modID(name), potion);
	}
}