package builderb0y.bigglobe.items;

import java.util.EnumMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.fabricmc.fabric.api.registry.FuelRegistry;

import net.minecraft.block.Block;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.TagEntry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.versions.ItemStackVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

import org.jetbrains.annotations.Nullable;

#if MC_VERSION > MC_1_19_2
	import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
	import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
#endif

#if MC_VERSION < MC_1_20_5
	import net.minecraft.loot.LootManager;
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
		OVERGROWN_SAND           = registerBlockPlacer(BigGlobeBlocks.OVERGROWN_SAND),
		OVERGROWN_PODZOL         = registerBlockPlacer(BigGlobeBlocks.OVERGROWN_PODZOL),
		ROSE                     = registerDecoPlacer(BigGlobeBlocks.ROSE),
		SHORT_GRASS              = registerDecoPlacer(BigGlobeBlocks.SHORT_GRASS),
		MUSHROOM_SPORES          = registerDecoPlacer(BigGlobeBlocks.MUSHROOM_SPORES),
		ROPE_ANCHOR              = registerDecoPlacer(BigGlobeBlocks.ROPE_ANCHOR),
		SPELUNKING_ROPE          = registerDecoPlacer(BigGlobeBlocks.SPELUNKING_ROPE),
		CRYSTALLINE_PRISMARINE   = registerBlockPlacer(BigGlobeBlocks.CRYSTALLINE_PRISMARINE),
		SLATED_PRISMARINE        = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE),
		SLATED_PRISMARINE_SLAB   = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE_SLAB),
		SLATED_PRISMARINE_STAIRS = registerBlockPlacer(BigGlobeBlocks.SLATED_PRISMARINE_STAIRS),
		ROCK                     = register("rock", new RockItem(BigGlobeBlocks.ROCK, settings(LegacyItemGroup.DECORATIONS))),
		ASHEN_NETHERRACK         = registerBlockPlacer(BigGlobeBlocks.ASHEN_NETHERRACK),
		SULFUR_ORE               = registerBlockPlacer(BigGlobeBlocks.SULFUR_ORE),
		SULFUR_BLOCK             = registerBlockPlacer(BigGlobeBlocks.SULFUR_BLOCK),
		WART_WEED                = registerDecoPlacer(BigGlobeBlocks.WART_WEED),
		CHARRED_GRASS            = registerDecoPlacer(BigGlobeBlocks.CHARRED_GRASS),
		BLAZING_BLOSSOM          = registerDecoPlacer(BigGlobeBlocks.BLAZING_BLOSSOM),
		GLOWING_GOLDENROD        = registerDecoPlacer(BigGlobeBlocks.GLOWING_GOLDENROD),
		CHARRED_PLANKS           = registerBlockPlacer(BigGlobeBlocks.CHARRED_PLANKS),
		CHARRED_SAPLING          = registerDecoPlacer(BigGlobeBlocks.CHARRED_SAPLING),
		CHARRED_LOG              = registerBlockPlacer(BigGlobeBlocks.CHARRED_LOG),
		STRIPPED_CHARRED_LOG     = registerBlockPlacer(BigGlobeBlocks.STRIPPED_CHARRED_LOG),
		CHARRED_WOOD             = registerBlockPlacer(BigGlobeBlocks.CHARRED_WOOD),
		STRIPPED_CHARRED_WOOD    = registerBlockPlacer(BigGlobeBlocks.STRIPPED_CHARRED_WOOD),
		CHARRED_LEAVES           = registerDecoPlacer(BigGlobeBlocks.CHARRED_LEAVES),
		CHARRED_SIGN             = register("charred_sign", new ColoredSignItem(settings(LegacyItemGroup.DECORATIONS).maxCount(16), BigGlobeBlocks.CHARRED_SIGN, BigGlobeBlocks.CHARRED_WALL_SIGN, DyeColor.LIGHT_GRAY)),
		#if MC_VERSION >= MC_1_20_0
		CHARRED_HANGING_SIGN     = register("charred_hanging_sign", new ColoredHangingSignItem(settings(LegacyItemGroup.DECORATIONS).maxCount(16), BigGlobeBlocks.CHARRED_HANGING_SIGN, BigGlobeBlocks.CHARRED_WALL_HANGING_SIGN, DyeColor.LIGHT_GRAY)),
		#endif
		CHARRED_PRESSURE_PLATE   = registerRedstonePlacer(BigGlobeBlocks.CHARRED_PRESSURE_PLATE),
		CHARRED_TRAPDOOR         = registerRedstonePlacer(BigGlobeBlocks.CHARRED_TRAPDOOR),
		CHARRED_STAIRS           = registerBlockPlacer(BigGlobeBlocks.CHARRED_STAIRS),
		CHARRED_BUTTON           = registerRedstonePlacer(BigGlobeBlocks.CHARRED_BUTTON),
		CHARRED_SLAB             = registerBlockPlacer(BigGlobeBlocks.CHARRED_SLAB),
		CHARRED_FENCE_GATE       = registerRedstonePlacer(BigGlobeBlocks.CHARRED_FENCE_GATE),
		CHARRED_FENCE            = registerDecoPlacer(BigGlobeBlocks.CHARRED_FENCE),
		CHARRED_DOOR             = register("charred_door", new TallBlockItem(BigGlobeBlocks.CHARRED_DOOR, settings(LegacyItemGroup.REDSTONE))),
		SOUL_MAGMA               = registerBlockPlacer(BigGlobeBlocks.SOUl_MAGMA),
		ROUGH_QUARTZ             = registerBlockPlacer(BigGlobeBlocks.ROUGH_QUARTZ),
		BUDDING_QUARTZ           = registerBlockPlacer(BigGlobeBlocks.BUDDING_QUARTZ),
		SMALL_QUARTZ_BUD         = registerDecoPlacer(BigGlobeBlocks.SMALL_QUARTZ_BUD),
		MEDIUM_QUARTZ_BUD        = registerDecoPlacer(BigGlobeBlocks.MEDIUM_QUARTZ_BUD),
		LARGE_QUARTZ_BUD         = registerDecoPlacer(BigGlobeBlocks.LARGE_QUARTZ_BUD),
		QUARTZ_CLUSTER           = registerDecoPlacer(BigGlobeBlocks.QUARTZ_CLUSTER),
		CHORUS_NYLIUM            = registerBlockPlacer(BigGlobeBlocks.CHORUS_NYLIUM),
		OVERGROWN_END_STONE      = registerBlockPlacer(BigGlobeBlocks.OVERGROWN_END_STONE),
		TALL_CHORUS_SPORES       = registerDecoPlacer(BigGlobeBlocks.TALL_CHORUS_SPORES),
		MEDIUM_CHORUS_SPORES     = registerDecoPlacer(BigGlobeBlocks.MEDIUM_CHORUS_SPORES),
		SHORT_CHORUS_SPORES      = registerDecoPlacer(BigGlobeBlocks.SHORT_CHORUS_SPORES);
	public static final EnumMap<CloudColor, BlockItem>
		CLOUDS      = new EnumMap<>(CloudColor.class),
		VOID_CLOUDS = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			CLOUDS     .put(color, registerDecoPlacer(BigGlobeBlocks.     CLOUDS.get(color)));
			VOID_CLOUDS.put(color, registerDecoPlacer(BigGlobeBlocks.VOID_CLOUDS.get(color)));
		}
	}
	public static final BlockItem[] MOLTEN_ROCKS = new BlockItem[8];
	static {
		for (int heat = 1; heat <= 8; heat++) {
			MOLTEN_ROCKS[heat - 1] = registerBlockPlacer(BigGlobeBlocks.MOLTEN_ROCKS[heat - 1]);
		}
	}

	public static final TorchArrowItem TORCH_ARROW = register(
		"torch_arrow",
		new TorchArrowItem(
			settings(LegacyItemGroup.COMBAT)
		)
	);
	public static final PercussiveHammerItem PERCUSSIVE_HAMMER = register(
		"percussive_hammer",
		new PercussiveHammerItem(
			2.0F,
			-2.8F,
			ToolMaterials.IRON,
			BigGlobeBlockTags.MINEABLE_PERCUSSIVE_HAMMER,
			settings(LegacyItemGroup.TOOLS)
			.maxDamage(166) //2/3'rds of the iron pickaxe durability, rounded down.
		)
	);
	public static final SlingshotItem SLINGSHOT = register(
		"slingshot",
		new SlingshotItem(
			settings(LegacyItemGroup.COMBAT).maxDamage(192)
		)
	);
	public static final BallOfStringItem BALL_OF_STRING = register(
		"ball_of_string",
		new BallOfStringItem(
			settings(LegacyItemGroup.TOOLS).maxCount(1)
		)
	);
	public static final Item ASH = register("ash", new Item(settings(LegacyItemGroup.BREWING)));
	public static final Item SULFUR = register("sulfur", new Item(settings(LegacyItemGroup.MATERIALS)));
	public static final BucketItem SOUL_LAVA_BUCKET = register(
		"soul_lava_bucket",
		new BucketItem(
			BigGlobeFluids.SOUL_LAVA,
			settings(LegacyItemGroup.TOOLS).recipeRemainder(Items.BUCKET).maxCount(1)
		)
	);
	public static final Item CHORUS_SPORE = register("chorus_spore", new Item(settings(LegacyItemGroup.BREWING)));
	public static final @Nullable WaypointItem
		PUBLIC_WAYPOINT  = BigGlobeConfig.INSTANCE.get().hyperspaceEnabled ? register("public_waypoint",  new WaypointItem(settings(LegacyItemGroup.TOOLS), false)) : null,
		PRIVATE_WAYPOINT = BigGlobeConfig.INSTANCE.get().hyperspaceEnabled ? register("private_waypoint", new WaypointItem(settings(LegacyItemGroup.TOOLS), true )) : null;
	public static final EnumMap<CloudColor, AuraBottleItem> AURA_BOTTLES = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			if (color != CloudColor.BLANK) {
				AURA_BOTTLES.put(color, register(color.bottleName, new AuraBottleItem(settings(LegacyItemGroup.MATERIALS), color)));
			}
		}
	}

	static { BigGlobeMod.LOGGER.debug("Done registering items."); }

	public static BlockItem registerBlockPlacer(Block block) {
		return registerPlacer(block, LegacyItemGroup.BUILDING_BLOCKS);
	}

	public static BlockItem registerDecoPlacer(Block block) {
		return registerPlacer(block, LegacyItemGroup.DECORATIONS);
	}

	public static BlockItem registerRedstonePlacer(Block block) {
		return registerPlacer(block, LegacyItemGroup.REDSTONE);
	}

	public static BlockItem registerPlacer(Block block, LegacyItemGroup group) {
		return Registry.register(
			RegistryVersions.item(),
			RegistryVersions.block().getId(block),
			new BlockItem(block, settings(group))
		);
	}

	public static <I extends Item> I register(String name, I item) {
		return Registry.register(RegistryVersions.item(), BigGlobeMod.modID(name), item);
	}

	public static Item.Settings settings(LegacyItemGroup group) {
		return (
			new Item.Settings()
			#if MC_VERSION <= MC_1_19_2
			.group(
				switch (group) {
					case BUILDING_BLOCKS -> ItemGroup.BUILDING_BLOCKS;
					case DECORATIONS -> ItemGroup.DECORATIONS;
					case REDSTONE -> ItemGroup.REDSTONE;
					case TRANSPORTATION -> ItemGroup.TRANSPORTATION;
					case MISC -> ItemGroup.MISC;
					case FOOD -> ItemGroup.FOOD;
					case TOOLS -> ItemGroup.TOOLS;
					case COMBAT -> ItemGroup.COMBAT;
					case BREWING -> ItemGroup.BREWING;
					case MATERIALS -> ItemGroup.MATERIALS;
				}
			)
			#endif
		);
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
		#if MC_VERSION > MC_1_19_2
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
				#if MC_VERSION >= MC_1_20_0
					entries.addAfter(Items.WARPED_HANGING_SIGN, CHARRED_HANGING_SIGN);
				#endif
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
			});
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register((FabricItemGroupEntries entries) -> {
				entries.addAfter(Items.CHARCOAL, SULFUR);
				entries.addAfter(Items.GUNPOWDER, ASH);
				entries.addAfter(Items.FLINT, ROCK);
				entries.addAfter(Items.NETHER_WART, CHORUS_SPORE);
				entries.addAfter(Items.EXPERIENCE_BOTTLE, AURA_BOTTLES.values().toArray(Item[]::new));
			});
			ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((FabricItemGroupEntries entries) -> {
				entries.addAfter(Items.CROSSBOW, SLINGSHOT);
			});
		#endif
	}

	public static ItemStack string(int blocks) {
		ItemStack stack = new ItemStack(BALL_OF_STRING);
		ItemStackVersions.setMaxDamage(stack, blocks);
		return stack;
	}

	public static enum LegacyItemGroup {
		BUILDING_BLOCKS,
		DECORATIONS,
		REDSTONE,
		TRANSPORTATION,
		MISC,
		FOOD,
		TOOLS,
		COMBAT,
		BREWING,
		MATERIALS;
	}
}