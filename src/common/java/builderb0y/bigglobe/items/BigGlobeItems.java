package builderb0y.bigglobe.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.FuelValueEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockdefs.*;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.versions.ItemStackVersions;

public class BigGlobeItems {

	static {
		BigGlobeMod.LOGGER.debug("Registering items...");
	}

	/*
	@TestOnly
	@Deprecated
	public static final TestItem
		TEST_ITEM                = register("test_item", new TestItem());
	//*/
	@SuppressWarnings("unused")
	public static final BlockItem
		OVERGROWN_SAND = registerPlacer(OverworldBlocks.OVERGROWN_SAND),
		OVERGROWN_PODZOL = registerPlacer(OverworldBlocks.OVERGROWN_PODZOL),
		ROSE = registerPlacer(OverworldBlocks.ROSE),
		SHORT_GRASS = registerPlacer(OverworldBlocks.SHORT_GRASS),
		MUSHROOM_SPORES = registerPlacer(OverworldBlocks.MUSHROOM_SPORES),
		ROPE_ANCHOR = registerPlacer(BigGlobeBlocks.ROPE_ANCHOR),
		SPELUNKING_ROPE = register(
			"spelunking_rope",
			new BlockItem(
				BigGlobeBlocks.SPELUNKING_ROPE,
				settings(BigGlobeBlocks.SPELUNKING_ROPE)

					.component(
						DataComponents.LORE,
						lore("item.bigglobe.spelunking_rope.tooltip")
					)

			)
		),
		CRYSTALLINE_PRISMARINE = registerPlacer(OverworldBlocks.CRYSTALLINE_PRISMARINE),
		SLATED_PRISMARINE = registerPlacer(OverworldBlocks.SLATED_PRISMARINE),
		SLATED_PRISMARINE_SLAB = registerPlacer(OverworldBlocks.SLATED_PRISMARINE_SLAB),
		SLATED_PRISMARINE_STAIRS = registerPlacer(OverworldBlocks.SLATED_PRISMARINE_STAIRS),
		ROCK = register("rock", new RockItem(OverworldBlocks.ROCK, settings(OverworldBlocks.ROCK))),
		ANCIENT_AUTOMATA = registerPlacer(OverworldBlocks.ANCIENT_AUTOMATA),
		AUTOMATA = registerPlacer(OverworldBlocks.AUTOMATA),
		RED_WILDFLOWERS = registerPlacer(OverworldBlocks.RED_WILDFLOWERS),
		BLUEBONNETS = registerPlacer(OverworldBlocks.BLUEBONNETS),
		VIOLETS = registerPlacer(OverworldBlocks.VIOLETS),
		ASHEN_NETHERRACK = registerPlacer(NetherBlocks.ASHEN_NETHERRACK),
		SULFUR_ORE = registerPlacer(NetherBlocks.SULFUR_ORE),
		SULFUR_BLOCK = registerPlacer(NetherBlocks.SULFUR_BLOCK),
		WART_WEED = registerPlacer(NetherBlocks.WART_WEED),
		CHARRED_GRASS = registerPlacer(NetherBlocks.CHARRED_GRASS),
		BLAZING_BLOSSOM = registerPlacer(NetherBlocks.BLAZING_BLOSSOM),
		SOUL_SILVERPETAL = registerPlacer(NetherBlocks.SOUL_SILVERPETAL),
		GLOWING_GOLDENROD = registerPlacer(NetherBlocks.GLOWING_GOLDENROD),
		CHARRED_PLANKS = registerPlacer(CharredBlocks.CHARRED_PLANKS),
		CHARRED_SAPLING = registerPlacer(CharredBlocks.CHARRED_SAPLING),
		CHARRED_LOG = registerPlacer(CharredBlocks.CHARRED_LOG),
		STRIPPED_CHARRED_LOG = registerPlacer(CharredBlocks.STRIPPED_CHARRED_LOG),
		CHARRED_WOOD = registerPlacer(CharredBlocks.CHARRED_WOOD),
		STRIPPED_CHARRED_WOOD = registerPlacer(CharredBlocks.STRIPPED_CHARRED_WOOD),
		CHARRED_LEAVES = registerPlacer(CharredBlocks.CHARRED_LEAVES),
		CHARRED_SIGN = register(
			"charred_sign",
			new ColoredSignItem(
				settings(CharredBlocks.CHARRED_SIGN).stacksTo(16),
				CharredBlocks.CHARRED_SIGN,
				CharredBlocks.CHARRED_WALL_SIGN,
				DyeColor.LIGHT_GRAY
			)
		),
		CHARRED_HANGING_SIGN = register(
			"charred_hanging_sign",
			new ColoredHangingSignItem(
				settings(CharredBlocks.CHARRED_HANGING_SIGN).stacksTo(16),
				CharredBlocks.CHARRED_HANGING_SIGN,
				CharredBlocks.CHARRED_WALL_HANGING_SIGN,
				DyeColor.LIGHT_GRAY
			)
		),
		CHARRED_PRESSURE_PLATE = registerPlacer(CharredBlocks.CHARRED_PRESSURE_PLATE),
		CHARRED_TRAPDOOR = registerPlacer(CharredBlocks.CHARRED_TRAPDOOR),
		CHARRED_STAIRS = registerPlacer(CharredBlocks.CHARRED_STAIRS),
		CHARRED_BUTTON = registerPlacer(CharredBlocks.CHARRED_BUTTON),
		CHARRED_SLAB = registerPlacer(CharredBlocks.CHARRED_SLAB),
		CHARRED_FENCE_GATE = registerPlacer(CharredBlocks.CHARRED_FENCE_GATE),
		CHARRED_FENCE = registerPlacer(CharredBlocks.CHARRED_FENCE),
		CHARRED_DOOR = register(
			"charred_door",
			new DoubleHighBlockItem(
				CharredBlocks.CHARRED_DOOR,
				settings(CharredBlocks.CHARRED_DOOR)
			)
		),

	CHARRED_SHELF = registerPlacer(CharredBlocks.CHARRED_SHELF),

	SOUL_MAGMA = registerPlacer(NetherBlocks.SOUl_MAGMA),
		ROUGH_QUARTZ = registerPlacer(NetherBlocks.ROUGH_QUARTZ),
		BUDDING_QUARTZ = registerPlacer(NetherBlocks.BUDDING_QUARTZ),
		SMALL_QUARTZ_BUD = registerPlacer(NetherBlocks.SMALL_QUARTZ_BUD),
		MEDIUM_QUARTZ_BUD = registerPlacer(NetherBlocks.MEDIUM_QUARTZ_BUD),
		LARGE_QUARTZ_BUD = registerPlacer(NetherBlocks.LARGE_QUARTZ_BUD),
		QUARTZ_CLUSTER = registerPlacer(NetherBlocks.QUARTZ_CLUSTER),
		PALE_NETHERRACK = registerPlacer(NetherBlocks.PALE_NETHERRACK),
		CHORUS_NYLIUM = registerPlacer(EndBlocks.CHORUS_NYLIUM),
		OVERGROWN_END_STONE = registerPlacer(EndBlocks.OVERGROWN_END_STONE),
		TALL_CHORUS_SPORES = registerPlacer(EndBlocks.TALL_CHORUS_SPORES),
		MEDIUM_CHORUS_SPORES = registerPlacer(EndBlocks.MEDIUM_CHORUS_SPORES),
		SHORT_CHORUS_SPORES = registerPlacer(EndBlocks.SHORT_CHORUS_SPORES),
		VOIDMETAL_BLOCK = registerPlacer(EndBlocks.VOIDMETAL_BLOCK);
	public static final EnumMap<CloudColor, BlockItem>
		CLOUDS = new EnumMap<>(CloudColor.class),
		VOID_CLOUDS = new EnumMap<>(CloudColor.class);

	static {
		for (CloudColor color : CloudColor.VALUES) {
			Block normalBlock = OverworldBlocks.CLOUDS.get(color);
			Block voidBlock = EndBlocks.VOID_CLOUDS.get(color);
			CLOUDS.put(color, registerPlacer(normalBlock));
			VOID_CLOUDS.put(color, registerPlacer(voidBlock));
		}
	}

	public static final BlockItem[] MOLTEN_ROCKS = new BlockItem[8];

	static {
		for (int heat = 1; heat <= 8; heat++) {
			MOLTEN_ROCKS[heat - 1] = registerPlacer(OverworldBlocks.MOLTEN_ROCKS[heat - 1]);
		}
	}

	public static final TorchArrowItem TORCH_ARROW = register(
		"torch_arrow",
		new TorchArrowItem(settings("torch_arrow"))
	);
	public static final PercussiveHammerItem PERCUSSIVE_HAMMER = register(
		"percussive_hammer",

		new PercussiveHammerItem(
			settings("percussive_hammer")
				.tool(
					ToolMaterial.IRON,
					BigGlobeBlockTags.MINEABLE_PERCUSSIVE_HAMMER,
					2.0F,
					-2.0F,
					0.0F
				)
				.durability(166) //2/3'rds of the iron pickaxe durability, rounded down.
		)

	);
	public static final SlingshotItem SLINGSHOT = register(
		"slingshot",
		new SlingshotItem(settings("slingshot").durability(192))
	);
	public static final BallOfStringItem BALL_OF_STRING = register(
		"ball_of_string",
		new BallOfStringItem(settings("ball_of_string").stacksTo(1))
	);
	public static final Item ASH = register("ash", new Item(settings("ash")));
	public static final Item SULFUR = register("sulfur", new Item(settings("sulfur")));
	public static final BucketItem SOUL_LAVA_BUCKET = register(
		"soul_lava_bucket",
		new BucketItem(
			BigGlobeFluids.SOUL_LAVA,
			settings("soul_lava_bucket")
				.craftRemainder(Items.BUCKET)
				.stacksTo(1)
		)
	);
	public static final Item CHORUS_SPORE = register("chorus_spore", new Item(settings("chorus_spore")));
	public static final WaypointItem
		PUBLIC_WAYPOINT = register("public_waypoint", new WaypointItem(settings("public_waypoint"), false)),
		PRIVATE_WAYPOINT = register("private_waypoint", new WaypointItem(settings("private_waypoint"), true));
	public static final EnumMap<CloudColor, AuraBottleItem> AURA_BOTTLES = new EnumMap<>(CloudColor.class);

	static {
		for (CloudColor color : CloudColor.VALUES) {
			if (color != CloudColor.BLANK) {
				AURA_BOTTLES.put(color, register(color.bottleName, new AuraBottleItem(settings(color.bottleName), color)));
			}
		}
	}

	public static final Item VOIDMETAL_INGOT = register("voidmetal_ingot", new Item(settings("voidmetal_ingot")));
	public static final SmithingTemplateItem VOIDMETAL_UPGRADE = register(
		"voidmetal_upgrade",
		new SmithingTemplateItem(
			Component.translatable("item.bigglobe.voidmetal_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
			Component.translatable("item.bigglobe.voidmetal_upgrade.ingredients").withStyle(ChatFormatting.BLUE),

			Component.translatable("item.bigglobe.voidmetal_upgrade.base_slot_description"),
			Component.translatable("item.bigglobe.voidmetal_upgrade.additions_slot_description"),
			Arrays.asList(
				BigGlobeMod.mcID("item/empty_armor_slot_helmet"),
				BigGlobeMod.mcID("item/empty_armor_slot_chestplate"),
				BigGlobeMod.mcID("item/empty_armor_slot_leggings"),
				BigGlobeMod.mcID("item/empty_armor_slot_boots")
			),
			Collections.singletonList(
				BigGlobeMod.mcID("item/empty_slot_ingot")
			)

			, new Item.Properties()
				.setId(key("voidmetal_upgrade"))
				.rarity(Rarity.UNCOMMON)

		)
	);

	public static final Item
		VOIDMETAL_HELMET = register("voidmetal_helmet", new Item(settings("voidmetal_helmet").humanoidArmor(VoidmetalArmorMaterial.INSTANCE, ArmorType.HELMET))),
		VOIDMETAL_CHESTPLATE = register("voidmetal_chestplate", new Item(settings("voidmetal_chestplate").humanoidArmor(VoidmetalArmorMaterial.INSTANCE, ArmorType.CHESTPLATE))),
		VOIDMETAL_LEGGINGS = register("voidmetal_leggings", new Item(settings("voidmetal_leggings").humanoidArmor(VoidmetalArmorMaterial.INSTANCE, ArmorType.LEGGINGS))),
		VOIDMETAL_BOOTS = register("voidmetal_boots", new Item(settings("voidmetal_boots").humanoidArmor(VoidmetalArmorMaterial.INSTANCE, ArmorType.BOOTS)));

	static {
		BigGlobeMod.LOGGER.debug("Done registering items.");
	}

	public static ItemLore lore(String key) {
		List<Component> list = Collections.singletonList(Component.translatable(key));
		return new ItemLore(list, list);
	}

	public static ResourceKey<Item> key(String name) {
		return ResourceKey.create(Registries.ITEM, BigGlobeMod.modID(name));
	}

	public static Item.Properties settings(String name) {

		return new Item.Properties().setId(key(name));
	}

	public static Item.Properties settings(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		Item.Properties settings = new Item.Properties();

		settings = (
			settings
				.setId(ResourceKey.create(Registries.ITEM, id))
				.useBlockDescriptionPrefix()
		);

		return settings;
	}

	public static BlockItem registerPlacer(Block block) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		Item.Properties settings = new Item.Properties();

		settings = (
			settings
				.setId(ResourceKey.create(Registries.ITEM, id))
				.useBlockDescriptionPrefix()
		);

		return Registry.register(
			BuiltInRegistries.ITEM,
			id,
			new BlockItem(block, settings)
		);
	}

	public static <I extends Item> I register(String name, I item) {
		Identifier id = BigGlobeMod.modID(name);

		if (!item.getDescriptionId().equals(Util.makeDescriptionId(item instanceof BlockItem ? "block" : "item", id))) {
			throw new IllegalArgumentException("Name mismatch");
		}

		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	public static void init() {
		FuelValueEvents.BUILD.register((FuelValues.Builder builder, FuelValueEvents.Context context) -> {
			int baseTime = context.baseSmeltTime();
			builder.add(SOUL_LAVA_BUCKET, baseTime * 100);
			builder.add(SULFUR, baseTime * 6);
			builder.add(SULFUR_BLOCK, baseTime * 60);
		});

		LootTableEvents.MODIFY.register(
			(
				ResourceKey<LootTable> id,
				LootTable.Builder tableBuilder,
				LootTableSource source,
				HolderLookup.Provider registries
			)
			-> {
				if (source.isBuiltin() && BuiltInLootTables.END_CITY_TREASURE.equals(id)) {
					tableBuilder.withPool(
						LootPool.lootPool().add(
							TagEntry
							.expandTag(BigGlobeItemTags.AURA_BOTTLES)
							.setWeight(100)
							.setQuality(1)
						)
					)
					.withPool(
						LootPool.lootPool().add(
							LootItem
							.lootTableItem(VOIDMETAL_UPGRADE)
							.setWeight(100)
							.when(
								LootItemRandomChanceCondition.randomChance(0.25F)
							)
						)
					);
				}
			}
		);
		CompostableRegistry.INSTANCE.add(
			SHORT_GRASS,
			CompostableRegistry.INSTANCE.get(
				Items.SHORT_GRASS
			)
			* 0.5F
		);
		Float wildflowerChance = CompostableRegistry.INSTANCE.get(Items.PINK_PETALS);
		CompostableRegistry.INSTANCE.add(RED_WILDFLOWERS, wildflowerChance);
		CompostableRegistry.INSTANCE.add(VIOLETS, wildflowerChance);
		CompostableRegistry.INSTANCE.add(BLUEBONNETS, wildflowerChance);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.WARPED_BUTTON, CHARRED_LOG, CHARRED_WOOD, STRIPPED_CHARRED_LOG, STRIPPED_CHARRED_WOOD, CHARRED_PLANKS, CHARRED_STAIRS, CHARRED_SLAB, CHARRED_FENCE, CHARRED_FENCE_GATE, CHARRED_DOOR, CHARRED_TRAPDOOR, CHARRED_PRESSURE_PLATE, CHARRED_BUTTON);
			entries.insertAfter(Items.DARK_PRISMARINE_SLAB, SLATED_PRISMARINE, SLATED_PRISMARINE_STAIRS, SLATED_PRISMARINE_SLAB);
			entries.insertBefore(Items.COAL_BLOCK, SULFUR_BLOCK);
			entries.insertAfter(Items.NETHERITE_BLOCK, VOIDMETAL_BLOCK);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.GRASS_BLOCK, OVERGROWN_PODZOL);
			entries.insertBefore(Items.SAND, OVERGROWN_SAND);
			entries.insertAfter(Items.PRISMARINE, CRYSTALLINE_PRISMARINE, SLATED_PRISMARINE);
			entries.insertBefore(Items.MAGMA_BLOCK, MOLTEN_ROCKS);
			entries.insertAfter(Items.MAGMA_BLOCK, SOUL_MAGMA);
			entries.insertAfter(Items.WARPED_NYLIUM, ASHEN_NETHERRACK, PALE_NETHERRACK);
			entries.insertAfter(Items.NETHER_QUARTZ_ORE, SULFUR_ORE);
			entries.insertAfter(Items.AMETHYST_CLUSTER, ROUGH_QUARTZ, BUDDING_QUARTZ, SMALL_QUARTZ_BUD, MEDIUM_QUARTZ_BUD, LARGE_QUARTZ_BUD, QUARTZ_CLUSTER);
			entries.insertAfter(Items.WARPED_STEM, CHARRED_LOG);
			entries.insertAfter(Items.FLOWERING_AZALEA_LEAVES, CHARRED_LEAVES);
			entries.insertAfter(Items.FLOWERING_AZALEA, CHARRED_SAPLING);
			entries.insertBefore(Items.BROWN_MUSHROOM, MUSHROOM_SPORES);
			entries.insertBefore(Items.SHORT_GRASS, SHORT_GRASS);
			entries.insertAfter(Items.DEAD_BUSH, CHARRED_GRASS);
			entries.insertAfter(Items.DANDELION, ROSE);
			entries.insertAfter(Items.OPEN_EYEBLOSSOM, BLAZING_BLOSSOM, SOUL_SILVERPETAL, GLOWING_GOLDENROD);

			entries.insertAfter(Items.WILDFLOWERS, RED_WILDFLOWERS, BLUEBONNETS, VIOLETS);

			entries.insertBefore(Items.CRIMSON_ROOTS, WART_WEED);
			entries.insertAfter(Items.STONE, ROCK);
			entries.insertAfter(Items.END_STONE, OVERGROWN_END_STONE, CHORUS_NYLIUM);
			entries.insertAfter(Items.NETHER_WART, CHORUS_SPORE);
			entries.insertBefore(Items.CHORUS_PLANT, SHORT_CHORUS_SPORES, MEDIUM_CHORUS_SPORES, TALL_CHORUS_SPORES);
			CLOUDS.values().stream().map(BlockItem::getDefaultInstance).forEachOrdered(entries::accept);
			VOID_CLOUDS.values().stream().map(BlockItem::getDefaultInstance).forEachOrdered(entries::accept);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.COPPER_CHAIN.waxedOxidized(), ROPE_ANCHOR, SPELUNKING_ROPE);
			entries.insertAfter(Items.MAGMA_BLOCK, SOUL_MAGMA);
			entries.insertAfter(Items.WARPED_HANGING_SIGN, CHARRED_SIGN, CHARRED_HANGING_SIGN);

			entries.insertAfter(Items.WARPED_SHELF, CHARRED_SHELF);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.REDSTONE_ORE, ANCIENT_AUTOMATA, AUTOMATA);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertBefore(Items.BUCKET, PERCUSSIVE_HAMMER);
			entries.insertAfter(Items.LAVA_BUCKET, SOUL_LAVA_BUCKET);
			entries.insertAfter(Items.FISHING_ROD, ROPE_ANCHOR, SPELUNKING_ROPE, TORCH_ARROW);
			entries.insertAfter(Items.LEAD, string(16), string(64), string(256));
			entries.insertAfter(Items.ENDER_EYE, PRIVATE_WAYPOINT);
			entries.insertAfter(Items.ENDER_EYE, PUBLIC_WAYPOINT);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.SPECTRAL_ARROW, TORCH_ARROW);
			entries.insertAfter(Items.NETHERITE_BOOTS, VOIDMETAL_HELMET, VOIDMETAL_CHESTPLATE, VOIDMETAL_LEGGINGS, VOIDMETAL_BOOTS);
			entries.insertAfter(Items.CROSSBOW, SLINGSHOT);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register((FabricCreativeModeTabOutput entries) -> {
			entries.insertAfter(Items.CHARCOAL, SULFUR);
			entries.insertAfter(Items.GUNPOWDER, ASH);
			entries.insertAfter(Items.FLINT, ROCK);
			entries.insertAfter(Items.NETHER_WART, CHORUS_SPORE);
			entries.insertAfter(Items.EXPERIENCE_BOTTLE, AURA_BOTTLES.values().toArray(Item[]::new));
			entries.insertAfter(Items.NETHERITE_INGOT, VOIDMETAL_INGOT);
			entries.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, VOIDMETAL_UPGRADE);
		});
	}

	public static ItemStack string(int blocks) {
		ItemStack stack = new ItemStack(BALL_OF_STRING);
		ItemStackVersions.setMaxDamage(stack, blocks);
		return stack;
	}
}