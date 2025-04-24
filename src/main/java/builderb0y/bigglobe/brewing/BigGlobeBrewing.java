package builderb0y.bigglobe.brewing;

import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.items.BigGlobeItems;

#if MC_VERSION >= MC_1_20_5
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
#endif

public class BigGlobeBrewing {

	static { BigGlobeMod.LOGGER.debug("Registering potions..."); }

	public static final RegistryEntry<StatusEffect>
		SOUL_SIPHON = registerEffect(
			"soul_siphon",
			new StatusEffect(StatusEffectCategory.HARMFUL, 0x00FFFF) {}
			.addAttributeModifier(
				#if MC_VERSION > MC_1_21_1
					EntityAttributes.MAX_HEALTH,
				#else
					EntityAttributes.GENERIC_MAX_HEALTH,
				#endif

				#if MC_VERSION < MC_1_21_0
					"0c5814dc-2112-41b0-9cd9-7966a2685f9d",
				#else
					BigGlobeMod.modID("effect.soul_siphon"),
				#endif

				-4.0D,

				#if MC_VERSION >= MC_1_20_5
					Operation.ADD_VALUE
				#else
					Operation.ADDITION
				#endif
			)
		);

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

	public static RegistryEntry<StatusEffect> registerEffect(String name, StatusEffect effect) {
		return Registry.registerReference(Registries.STATUS_EFFECT, BigGlobeMod.modID(name), effect);
	}

	public static #if MC_VERSION >= MC_1_20_5 RegistryEntry<Potion> #else Potion #endif register(String name, Potion potion) {
		return Registry. #if MC_VERSION >= MC_1_20_5 registerReference #else register #endif (Registries.POTION, BigGlobeMod.modID(name), potion);
	}
}