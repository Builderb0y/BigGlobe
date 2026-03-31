package builderb0y.bigglobe.brewing;

import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.items.BigGlobeItems;

public class BigGlobeBrewing {

	static {
		BigGlobeMod.LOGGER.debug("Registering potions...");
	}

	public static final Holder<MobEffect>
		SOUL_SIPHON = registerEffect(
		"soul_siphon",
		new MobEffect(MobEffectCategory.HARMFUL, 0x00FFFF) {}
		.addAttributeModifier(
			Attributes.MAX_HEALTH,
			BigGlobeMod.modID("effect.soul_siphon"),
			-4.0D,
			Operation.ADD_VALUE
		)
	);

	public static final Holder<Potion>
		WITHER = register("wither", new Potion("wither", new MobEffectInstance(MobEffects.WITHER, 600, 0))),
		LONG_WITHER = register("long_wither", new Potion("long_wither", new MobEffectInstance(MobEffects.WITHER, 1200, 0))),
		STRONG_WITHER = register("strong_wither", new Potion("strong_wither", new MobEffectInstance(MobEffects.WITHER, 300, 1)));

	static {
		BigGlobeMod.LOGGER.debug("Done registering potions.");
	}

	public static void init() {
		FabricPotionBrewingBuilder.BUILD.register((PotionBrewing.Builder builder) -> {
			BigGlobeMod.LOGGER.debug("Registering potion recipes...");
			builder.addMix(Potions.AWKWARD, BigGlobeItems.ASH, WITHER);
			builder.addMix(WITHER, Items.REDSTONE, LONG_WITHER);
			builder.addMix(WITHER, Items.GLOWSTONE_DUST, STRONG_WITHER);
			builder.addMix(Potions.WATER, BigGlobeItems.CHORUS_SPORE, Potions.AWKWARD);
			BigGlobeMod.LOGGER.debug("Done registering potion recipes.");
		});
	}

	public static Holder<MobEffect> registerEffect(String name, MobEffect effect) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BigGlobeMod.modID(name), effect);
	}

	public static Holder<Potion> register(String name, Potion potion) {
		return Registry.registerForHolder(BuiltInRegistries.POTION, BigGlobeMod.modID(name), potion);
	}
}