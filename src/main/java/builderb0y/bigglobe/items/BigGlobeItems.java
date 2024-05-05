package builderb0y.bigglobe.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.TagEntry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.versions.ItemStackVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION < MC_1_20_5
	import net.minecraft.loot.LootManager;
import net.minecraft.util.Identifier;
#endif

public class BigGlobeItems {

	static { BigGlobeMod.LOGGER.debug("Registering items..."); }

	/*
	@TestOnly
	@Deprecated
	public static final TestItem
		TEST_ITEM                = register("test_item", new TestItem());
	//*/
	@SuppressWarnings("unused")
	public static final BlockItem
		OVERGROWN_SAND           = registerPlacer(BigGlobeBlocks.OVERGROWN_SAND),
		OVERGROWN_PODZOL         = registerPlacer(BigGlobeBlocks.OVERGROWN_PODZOL),
		ROSE                     = registerPlacer(BigGlobeBlocks.ROSE),
		SHORT_GRASS              = registerPlacer(BigGlobeBlocks.SHORT_GRASS),
		MUSHROOM_SPORES          = registerPlacer(BigGlobeBlocks.MUSHROOM_SPORES),
		ROPE_ANCHOR              = registerPlacer(BigGlobeBlocks.ROPE_ANCHOR),
		SPELUNKING_ROPE          = registerPlacer(BigGlobeBlocks.SPELUNKING_ROPE),
		CRYSTALLINE_PRISMARINE   = registerPlacer(BigGlobeBlocks.CRYSTALLINE_PRISMARINE),
		SLATED_PRISMARINE        = registerPlacer(BigGlobeBlocks.SLATED_PRISMARINE),
		SLATED_PRISMARINE_SLAB   = registerPlacer(BigGlobeBlocks.SLATED_PRISMARINE_SLAB),
		SLATED_PRISMARINE_STAIRS = registerPlacer(BigGlobeBlocks.SLATED_PRISMARINE_STAIRS),
		ROCK                     = register("rock", new RockItem(BigGlobeBlocks.ROCK, new Item.Settings())),
		ASHEN_NETHERRACK         = registerPlacer(BigGlobeBlocks.ASHEN_NETHERRACK),
		SULFUR_ORE               = registerPlacer(BigGlobeBlocks.SULFUR_ORE),
		SULFUR_BLOCK             = registerPlacer(BigGlobeBlocks.SULFUR_BLOCK),
		WART_WEED                = registerPlacer(BigGlobeBlocks.WART_WEED),
		CHARRED_GRASS            = registerPlacer(BigGlobeBlocks.CHARRED_GRASS),
		BLAZING_BLOSSOM          = registerPlacer(BigGlobeBlocks.BLAZING_BLOSSOM),
		GLOWING_GOLDENROD        = registerPlacer(BigGlobeBlocks.GLOWING_GOLDENROD),
		CHARRED_PLANKS           = registerPlacer(BigGlobeBlocks.CHARRED_PLANKS),
		CHARRED_SAPLING          = registerPlacer(BigGlobeBlocks.CHARRED_SAPLING),
		CHARRED_LOG              = registerPlacer(BigGlobeBlocks.CHARRED_LOG),
		STRIPPED_CHARRED_LOG     = registerPlacer(BigGlobeBlocks.STRIPPED_CHARRED_LOG),
		CHARRED_WOOD             = registerPlacer(BigGlobeBlocks.CHARRED_WOOD),
		STRIPPED_CHARRED_WOOD    = registerPlacer(BigGlobeBlocks.STRIPPED_CHARRED_WOOD),
		CHARRED_LEAVES           = registerPlacer(BigGlobeBlocks.CHARRED_LEAVES),
		CHARRED_SIGN             = register("charred_sign", new ColoredSignItem(new Item.Settings().maxCount(16), BigGlobeBlocks.CHARRED_SIGN, BigGlobeBlocks.CHARRED_WALL_SIGN, DyeColor.LIGHT_GRAY)),
		CHARRED_HANGING_SIGN     = register("charred_hanging_sign", new ColoredHangingSignItem(new Item.Settings().maxCount(16), BigGlobeBlocks.CHARRED_HANGING_SIGN, BigGlobeBlocks.CHARRED_WALL_HANGING_SIGN, DyeColor.LIGHT_GRAY)),
		CHARRED_PRESSURE_PLATE   = registerPlacer(BigGlobeBlocks.CHARRED_PRESSURE_PLATE),
		CHARRED_TRAPDOOR         = registerPlacer(BigGlobeBlocks.CHARRED_TRAPDOOR),
		CHARRED_STAIRS           = registerPlacer(BigGlobeBlocks.CHARRED_STAIRS),
		CHARRED_BUTTON           = registerPlacer(BigGlobeBlocks.CHARRED_BUTTON),
		CHARRED_SLAB             = registerPlacer(BigGlobeBlocks.CHARRED_SLAB),
		CHARRED_FENCE_GATE       = registerPlacer(BigGlobeBlocks.CHARRED_FENCE_GATE),
		CHARRED_FENCE            = registerPlacer(BigGlobeBlocks.CHARRED_FENCE),
		CHARRED_DOOR             = register("charred_door", new TallBlockItem(BigGlobeBlocks.CHARRED_DOOR, new Item.Settings())),
		SOUL_MAGMA               = registerPlacer(BigGlobeBlocks.SOUl_MAGMA),
		ROUGH_QUARTZ             = registerPlacer(BigGlobeBlocks.ROUGH_QUARTZ),
		BUDDING_QUARTZ           = registerPlacer(BigGlobeBlocks.BUDDING_QUARTZ),
		SMALL_QUARTZ_BUD         = registerPlacer(BigGlobeBlocks.SMALL_QUARTZ_BUD),
		MEDIUM_QUARTZ_BUD        = registerPlacer(BigGlobeBlocks.MEDIUM_QUARTZ_BUD),
		LARGE_QUARTZ_BUD         = registerPlacer(BigGlobeBlocks.LARGE_QUARTZ_BUD),
		QUARTZ_CLUSTER           = registerPlacer(BigGlobeBlocks.QUARTZ_CLUSTER),
		CHORUS_NYLIUM            = registerPlacer(BigGlobeBlocks.CHORUS_NYLIUM),
		OVERGROWN_END_STONE      = registerPlacer(BigGlobeBlocks.OVERGROWN_END_STONE),
		TALL_CHORUS_SPORES       = registerPlacer(BigGlobeBlocks.TALL_CHORUS_SPORES),
		MEDIUM_CHORUS_SPORES     = registerPlacer(BigGlobeBlocks.MEDIUM_CHORUS_SPORES),
		SHORT_CHORUS_SPORES      = registerPlacer(BigGlobeBlocks.SHORT_CHORUS_SPORES);
	public static final EnumMap<CloudColor, BlockItem>
		CLOUDS      = new EnumMap<>(CloudColor.class),
		VOID_CLOUDS = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			Block block1 = BigGlobeBlocks.     CLOUDS.get(color);
			CLOUDS     .put(color, registerPlacer(block1));
			Block block = BigGlobeBlocks.VOID_CLOUDS.get(color);
			VOID_CLOUDS.put(color, registerPlacer(block));
		}
	}
	public static final BlockItem[] MOLTEN_ROCKS = new BlockItem[8];
	static {
		for (int heat = 1; heat <= 8; heat++) {
			MOLTEN_ROCKS[heat - 1] = registerPlacer(BigGlobeBlocks.MOLTEN_ROCKS[heat - 1]);
		}
	}

	public static final TorchArrowItem TORCH_ARROW = register(
		"torch_arrow",
		new TorchArrowItem(new Item.Settings())
	);
	public static final PercussiveHammerItem PERCUSSIVE_HAMMER = register(
		"percussive_hammer",
		new PercussiveHammerItem(
			2.0F,
			-2.8F,
			ToolMaterials.IRON,
			BigGlobeBlockTags.MINEABLE_PERCUSSIVE_HAMMER,
			new Item.Settings()
			.maxDamage(166) //2/3'rds of the iron pickaxe durability, rounded down.
		)
	);
	public static final SlingshotItem SLINGSHOT = register(
		"slingshot",
		new SlingshotItem(new Item.Settings().maxDamage(192))
	);
	public static final BallOfStringItem BALL_OF_STRING = register(
		"ball_of_string",
		new BallOfStringItem(new Item.Settings().maxCount(1))
	);
	public static final Item ASH = register("ash", new Item(new Item.Settings()));
	public static final Item SULFUR = register("sulfur", new Item(new Item.Settings()));
	public static final BucketItem SOUL_LAVA_BUCKET = register(
		"soul_lava_bucket",
		new BucketItem(
			BigGlobeFluids.SOUL_LAVA,
			new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1)
		)
	);
	public static final Item CHORUS_SPORE = register("chorus_spore", new Item(new Item.Settings()));
	public static final @Nullable WaypointItem
		PUBLIC_WAYPOINT  = register("public_waypoint",  new WaypointItem(new Item.Settings(), false)),
		PRIVATE_WAYPOINT = register("private_waypoint", new WaypointItem(new Item.Settings(), true ));
	public static final EnumMap<CloudColor, AuraBottleItem> AURA_BOTTLES = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			if (color != CloudColor.BLANK) {
				AURA_BOTTLES.put(color, register(color.bottleName, new AuraBottleItem(new Item.Settings(), color)));
			}
		}
	}
	public static final Item VOIDMETAL_INGOT = register("voidmetal_ingot", new Item(new Item.Settings()));
	public static final SmithingTemplateItem VOIDMETAL_UPGRADE = register(
		"voidmetal_upgrade",
		new SmithingTemplateItem(
			Text.translatable("item.bigglobe.voidmetal_upgrade.applies_to").formatted(Formatting.BLUE),
			Text.translatable("item.bigglobe.voidmetal_upgrade.ingredients").formatted(Formatting.BLUE),
			Text.translatable("upgrade.bigglobe.voidmetal_upgrade").formatted(Formatting.GRAY),
			Text.translatable("item.bigglobe.voidmetal_upgrade.base_slot_description"),
			Text.translatable("item.bigglobe.voidmetal_upgrade.additions_slot_description"),
			Arrays.asList(
				BigGlobeMod.mcID("item/empty_armor_slot_helmet"),
				BigGlobeMod.mcID("item/empty_armor_slot_chestplate"),
				BigGlobeMod.mcID("item/empty_armor_slot_leggings"),
				BigGlobeMod.mcID("item/empty_armor_slot_boots")
			),
			Collections.singletonList(
				BigGlobeMod.mcID("item/empty_slot_ingot")
			)
		)
	);
	public static final ArmorItem
		VOIDMETAL_HELMET     = register("voidmetal_helmet",     new ArmorItem(VoidmetalArmorMaterial.INSTANCE, ArmorItem.Type.HELMET,     new Item.Settings() #if MC_VERSION >= MC_1_20_5 .maxDamage(ArmorItem.Type.HELMET    .getMaxDamage(37)) #endif)),
		VOIDMETAL_CHESTPLATE = register("voidmetal_chestplate", new ArmorItem(VoidmetalArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Settings() #if MC_VERSION >= MC_1_20_5 .maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(37)) #endif)),
		VOIDMETAL_LEGGINGS   = register("voidmetal_leggings",   new ArmorItem(VoidmetalArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS,   new Item.Settings() #if MC_VERSION >= MC_1_20_5 .maxDamage(ArmorItem.Type.LEGGINGS  .getMaxDamage(37)) #endif)),
		VOIDMETAL_BOOTS      = register("voidmetal_boots",      new ArmorItem(VoidmetalArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS,      new Item.Settings() #if MC_VERSION >= MC_1_20_5 .maxDamage(ArmorItem.Type.BOOTS     .getMaxDamage(37)) #endif));

	static { BigGlobeMod.LOGGER.debug("Done registering items."); }

	public static BlockItem registerPlacer(Block block) {
		return Registry.register(
			RegistryVersions.item(),
			RegistryVersions.block().getId(block),
			new BlockItem(block, new Item.Settings())
		);
	}

	public static <I extends Item> I register(String name, I item) {
		return Registry.register(RegistryVersions.item(), BigGlobeMod.modID(name), item);
	}

	public static void init() {
		FuelRegistry.INSTANCE.add(SOUL_LAVA_BUCKET, 20000);
		FuelRegistry.INSTANCE.add(SULFUR, 1200);
		FuelRegistry.INSTANCE.add(SULFUR_BLOCK, 12000);
		LootTableEvents.MODIFY.register(
			(
				#if MC_VERSION >= MC_1_20_5
					RegistryKey<LootTable> id,
				#else
					ResourceManager resourceManager,
					LootManager lootManager,
					Identifier id,
				#endif
				LootTable.Builder tableBuilder,
				LootTableSource source
			)
			-> {
				if (source.isBuiltin() && LootTables.END_CITY_TREASURE_CHEST.equals(id)) {
					tableBuilder.pool(
						LootPool.builder().with(
							TagEntry
							.expandBuilder(BigGlobeItemTags.AURA_BOTTLES)
							.weight(100)
							.quality(1)
						)
					)
					.pool(
						LootPool.builder().with(
							ItemEntry
							.builder(VOIDMETAL_UPGRADE)
							.weight(100)
							.conditionally(
								RandomChanceLootCondition.builder(0.25F)
							)
						)
					);
				}
			}
		);
	}

	@Environment(EnvType.CLIENT)
	@SuppressWarnings("UnstableApiUsage")
	public static void initClient() {
		ColorProviderRegistry.ITEM.register(
			(ItemStack stack, int tintIndex) -> GrassColors.getColor(0.5D, 1.0D),
			OVERGROWN_PODZOL,
			SHORT_GRASS
		);
		ModelPredicateProviderRegistry.register(SLINGSHOT, BigGlobeMod.modID("loaded"), (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) -> {
			if (entity == null || entity.getActiveItem() != stack) return 0.0F;
			return ((float)(stack.getMaxUseTime() - entity.getItemUseTimeLeft())) / 20.0F;
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.WARPED_BUTTON, CHARRED_LOG, CHARRED_WOOD, STRIPPED_CHARRED_LOG, STRIPPED_CHARRED_WOOD, CHARRED_PLANKS, CHARRED_STAIRS, CHARRED_SLAB, CHARRED_FENCE, CHARRED_FENCE_GATE, CHARRED_DOOR, CHARRED_TRAPDOOR, CHARRED_PRESSURE_PLATE, CHARRED_BUTTON);
			entries.addAfter(Items.DARK_PRISMARINE_SLAB, SLATED_PRISMARINE, SLATED_PRISMARINE_STAIRS, SLATED_PRISMARINE_SLAB);
			entries.addBefore(Items.COAL_BLOCK, SULFUR_BLOCK);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.GRASS_BLOCK, OVERGROWN_PODZOL);
			entries.addBefore(Items.SAND, OVERGROWN_SAND);
			entries.addAfter(Items.PRISMARINE, CRYSTALLINE_PRISMARINE, SLATED_PRISMARINE);
			entries.addBefore(Items.MAGMA_BLOCK, MOLTEN_ROCKS);
			entries.addAfter(Items.MAGMA_BLOCK, SOUL_MAGMA);
			entries.addAfter(Items.WARPED_NYLIUM, ASHEN_NETHERRACK);
			entries.addAfter(Items.NETHER_QUARTZ_ORE, SULFUR_ORE);
			entries.addAfter(Items.AMETHYST_CLUSTER, ROUGH_QUARTZ, BUDDING_QUARTZ, SMALL_QUARTZ_BUD, MEDIUM_QUARTZ_BUD, LARGE_QUARTZ_BUD, QUARTZ_CLUSTER);
			entries.addAfter(Items.WARPED_STEM, CHARRED_LOG);
			entries.addAfter(Items.FLOWERING_AZALEA_LEAVES, CHARRED_LEAVES);
			entries.addAfter(Items.FLOWERING_AZALEA, CHARRED_SAPLING);
			entries.addBefore(Items.BROWN_MUSHROOM, MUSHROOM_SPORES);
			entries.addBefore(Items.#if MC_VERSION >= MC_1_20_3 SHORT_GRASS #else GRASS #endif, SHORT_GRASS);
			entries.addAfter(Items.DEAD_BUSH, CHARRED_GRASS);
			entries.addAfter(Items.DANDELION, ROSE);
			entries.addAfter(Items.TORCHFLOWER, BLAZING_BLOSSOM, GLOWING_GOLDENROD);
			entries.addBefore(Items.CRIMSON_ROOTS, WART_WEED);
			entries.addAfter(Items.STONE, ROCK);
			entries.addAfter(Items.END_STONE, OVERGROWN_END_STONE, CHORUS_NYLIUM);
			entries.addAfter(Items.NETHER_WART, CHORUS_SPORE);
			entries.addBefore(Items.CHORUS_PLANT, SHORT_CHORUS_SPORES, MEDIUM_CHORUS_SPORES, TALL_CHORUS_SPORES);
			CLOUDS.values().stream().map(BlockItem::getDefaultStack).forEachOrdered(entries::add);
			VOID_CLOUDS.values().stream().map(BlockItem::getDefaultStack).forEachOrdered(entries::add);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.CHAIN, ROPE_ANCHOR, SPELUNKING_ROPE);
			entries.addAfter(Items.MAGMA_BLOCK, SOUL_MAGMA);
			entries.addAfter(Items.WARPED_SIGN, CHARRED_SIGN);
			entries.addAfter(Items.WARPED_HANGING_SIGN, CHARRED_HANGING_SIGN);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register((FabricItemGroupEntries entries) -> {
			entries.addBefore(Items.BUCKET, PERCUSSIVE_HAMMER);
			entries.addAfter(Items.LAVA_BUCKET, SOUL_LAVA_BUCKET);
			entries.addAfter(Items.FISHING_ROD, ROPE_ANCHOR, SPELUNKING_ROPE, TORCH_ARROW);
			entries.addAfter(Items.LEAD, string(16), string(64), string(256));
			if (PRIVATE_WAYPOINT != null) entries.addAfter(Items.ENDER_EYE, PRIVATE_WAYPOINT);
			if (PUBLIC_WAYPOINT  != null) entries.addAfter(Items.ENDER_EYE, PUBLIC_WAYPOINT );
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.SPECTRAL_ARROW, TORCH_ARROW);
			entries.addAfter(Items.NETHERITE_BOOTS, VOIDMETAL_HELMET, VOIDMETAL_CHESTPLATE, VOIDMETAL_LEGGINGS, VOIDMETAL_BOOTS);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.CHARCOAL, SULFUR);
			entries.addAfter(Items.GUNPOWDER, ASH);
			entries.addAfter(Items.FLINT, ROCK);
			entries.addAfter(Items.NETHER_WART, CHORUS_SPORE);
			entries.addAfter(Items.EXPERIENCE_BOTTLE, AURA_BOTTLES.values().toArray(Item[]::new));
			entries.addAfter(Items.NETHERITE_INGOT, VOIDMETAL_INGOT);
			entries.addAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, VOIDMETAL_UPGRADE);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((FabricItemGroupEntries entries) -> {
			entries.addAfter(Items.CROSSBOW, SLINGSHOT);
		});
	}

	public static ItemStack string(int blocks) {
		ItemStack stack = new ItemStack(BALL_OF_STRING);
		ItemStackVersions.setMaxDamage(stack, blocks);
		return stack;
	}
}