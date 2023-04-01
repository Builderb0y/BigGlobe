package builderb0y.bigglobe.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.sign.SignTypeRegistry;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.*;
import net.minecraft.block.AbstractBlock.OffsetType;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.HoeItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.SignType;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockEntities.MutableBlockEntityType;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.trees.SaplingGrowHandler;

public class BigGlobeBlocks {

	static { BigGlobeMod.LOGGER.debug("Registering blocks..."); }

	public static final SignType CHARRED_SIGN_TYPE = SignTypeRegistry.registerSignType(BigGlobeMod.modID("charred"));

	public static final OvergrownSandBlock OVERGROWN_SAND = register(
		"overgrown_sand",
		new OvergrownSandBlock(
			AbstractBlock.Settings
			.copy(Blocks.SAND)
			.ticksRandomly()
		)
	);
	public static final SnowyBlock OVERGROWN_PODZOL = register(
		"overgrown_podzol",
		new SnowyBlock(
			AbstractBlock.Settings
			.of(Material.SOIL, MapColor.DARK_GREEN)
			.strength(0.5F)
			.sounds(BlockSoundGroup.GRAVEL)
		)
	);
	public static final FlowerBlock ROSE = register(
		"rose",
		new FlowerBlock(
			StatusEffects.LUCK,
			5,
			AbstractBlock.Settings
			.of(Material.PLANT)
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offsetType(OffsetType.XZ)
		)
	);
	public static final FlowerPotBlock POTTED_ROSE = register("potted_rose", newPottedPlant(ROSE));
	public static final ShortGrassBlock SHORT_GRASS = register(
		"short_grass",
		new ShortGrassBlock(
			AbstractBlock.Settings
			.copy(Blocks.GRASS)
			.offsetType(OffsetType.XZ)
		)
	);
	public static final MushroomSporesBlock MUSHROOM_SPORES = register(
		"mushroom_spores",
		new MushroomSporesBlock(
			AbstractBlock.Settings
			.of(Material.REPLACEABLE_PLANT, MapColor.PURPLE)
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offsetType(OffsetType.XZ)
		)
	);
	public static final SpelunkingRopeBlock SPELUNKING_ROPE = register(
		"spelunking_rope",
		new SpelunkingRopeBlock(
			AbstractBlock.Settings
			.of(Material.WOOL, MapColor.OAK_TAN)
			.strength(0.8f)
			.sounds(BlockSoundGroup.WOOL)
		)
	);
	public static final RopeAnchorBlock ROPE_ANCHOR = register(
		"rope_anchor",
		new RopeAnchorBlock(
			AbstractBlock.Settings.of(Material.METAL, MapColor.IRON_GRAY)
			.requiresTool()
			.strength(5.0F)
			.sounds(BlockSoundGroup.DEEPSLATE_BRICKS)
		)
	);
	public static final Block CRYSTALLINE_PRISMARINE = register(
		"crystalline_prismarine",
		new Block(
			AbstractBlock.Settings.copy(Blocks.PRISMARINE)
			.luminance(state -> 4)
		)
	);
	public static final Block SLATED_PRISMARINE = register(
		"slated_prismarine",
		new Block(AbstractBlock.Settings.copy(Blocks.DARK_PRISMARINE))
	);
	public static final SlabBlock SLATED_PRISMARINE_SLAB = register(
		"slated_prismarine_slab",
		new SlabBlock(AbstractBlock.Settings.copy(SLATED_PRISMARINE))
	);
	public static final StairsBlock SLATED_PRISMARINE_STAIRS = register(
		"slated_prismarine_stairs",
		new StairsBlock(
			SLATED_PRISMARINE.getDefaultState(),
			AbstractBlock.Settings.copy(SLATED_PRISMARINE)
		)
	);
	public static final Block FLOATSTONE = register(
		"floatstone",
		new Block(
			AbstractBlock.Settings.of(Material.STONE, MapColor.OAK_TAN)
			.requiresTool()
			.strength(1.0F, 5.0F)
		)
	);
	public static final SlabBlock FLOATSTONE_SLAB = register(
		"floatstone_slab",
		new SlabBlock(AbstractBlock.Settings.copy(FLOATSTONE))
	);
	public static final StairsBlock FLOATSTONE_STAIRS = register(
		"floatstone_stairs",
		new StairsBlock(
			FLOATSTONE.getDefaultState(),
			AbstractBlock.Settings.copy(FLOATSTONE)
		)
	);
	public static final DelayedGenerationBlock DELAYED_GENERATION = register(
		"delayed_generation",
		new DelayedGenerationBlock(
			AbstractBlock.Settings.of(Material.PLANT)
			.breakInstantly()
			.noCollision()
			.nonOpaque()
			.dropsNothing()
		)
	);

	public static final AshenNetherrackBlock ASHEN_NETHERRACK = register(
		"ashen_netherrack",
		new AshenNetherrackBlock(
			AbstractBlock.Settings.of(Material.STONE, MapColor.BLACK)
			.requiresTool()
			.strength(0.4F)
			.sounds(BlockSoundGroup.NETHERRACK)
		)
	);
	public static final Block SULFUR_ORE = register(
		"sulfur_ore",
		new OreBlock(
			AbstractBlock.Settings.of(Material.STONE, MapColor.DARK_RED)
			.strength(3.0F)
			.requiresTool(),
			UniformIntProvider.create(0, 2)
		)
	);
	public static final Block SULFUR_BLOCK = register(
		"sulfur_block",
		new Block(
			AbstractBlock.Settings.of(Material.STONE, MapColor.YELLOW)
			.strength(5.0F, 6.0F)
			.requiresTool()
		)
	);
	public static final NetherGrassBlock WART_WEED = register(
		"wart_weed",
		new NetherGrassBlock(
			AbstractBlock.Settings.of(Material.NETHER_SHOOTS, MapColor.RED)
			.nonOpaque()
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offsetType(OffsetType.XZ)
		)
	);
	public static final NetherGrassBlock CHARRED_GRASS = register(
		"charred_grass",
		new NetherGrassBlock(
			AbstractBlock.Settings.of(Material.NETHER_SHOOTS, MapColor.BLACK)
			.nonOpaque()
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offsetType(OffsetType.XZ)
		)
	);
	public static final BlazingBlossomBlock BLAZING_BLOSSOM = register(
		"blazing_blossom",
		new BlazingBlossomBlock(
			StatusEffects.FIRE_RESISTANCE,
			8,
			AbstractBlock.Settings.of(Material.PLANT, MapColor.TERRACOTTA_ORANGE)
			.breakInstantly()
			.nonOpaque()
			.noCollision()
			.sounds(BlockSoundGroup.GRASS)
			.luminance(state -> 7)
		)
	);
	public static final NetherFlowerBlock GLOWING_GOLDENROD = register(
		"glowing_goldenrod",
		new NetherFlowerBlock(
			StatusEffects.GLOWING,
			8,
			AbstractBlock.Settings.of(Material.PLANT, MapColor.PALE_YELLOW)
			.breakInstantly()
			.nonOpaque()
			.noCollision()
			.sounds(BlockSoundGroup.GRASS)
			.luminance(state -> 11)
		)
	);
	public static final FlowerPotBlock POTTED_BLAZING_BLOSSOM = register("potted_blazing_blossom", newPottedPlant(BLAZING_BLOSSOM));
	public static final FlowerPotBlock POTTED_GLOWING_GOLDENROD = register("potted_glowing_goldenrod", newPottedPlant(GLOWING_GOLDENROD));
	public static final SoulLavaBlock SOUL_LAVA = register(
		"soul_lava",
		new SoulLavaBlock(
			BigGlobeFluids.SOUL_LAVA,
			AbstractBlock.Settings.of(Material.LAVA, MapColor.DIAMOND_BLUE)
			.noCollision()
			.ticksRandomly()
			.strength(100.0F)
			.luminance(state -> 15)
			.dropsNothing()
		)
	);
	public static final MagmaBlock SOUl_MAGMA = register(
		"soul_magma",
		new MagmaBlock(
			AbstractBlock.Settings.copy(Blocks.MAGMA_BLOCK)
			.mapColor(MapColor.LAPIS_BLUE)
			.allowsSpawning((state, world, pos, type) -> type.isFireImmune()) //not copied by copy().
		)
	);
	public static final SoulCauldronBlock SOUL_CAULDRON = register(
		"soul_cauldron",
		new SoulCauldronBlock(
			AbstractBlock.Settings.copy(Blocks.LAVA_CAULDRON)
		)
	);
	public static final Block CHARRED_PLANKS = register(
		"charred_planks",
		new Block(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.strength(2.0F, 3.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final SaplingBlock CHARRED_SAPLING = register(
		"charred_sapling",
		new CharredSaplingBlock(
			new SaplingGenerator() {

				public static final RegistryKey<ConfiguredFeature<?, ?>> KEY = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, BigGlobeMod.modID("charred_tree_vanilla"));

				/**
				note: the ConfiguredFeature returned by this method will be
				overridden in big globe worlds by {@link SaplingGrowHandler}.
				*/
				@Nullable
				@Override
				public RegistryEntry<? extends ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
					MinecraftServer server = BigGlobeMod.currentServer;
					return server == null ? null : server.getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).entryOf(KEY);
				}
			},
			AbstractBlock.Settings.of(Material.PLANT)
			.noCollision()
			.nonOpaque()
			.ticksRandomly()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
		)
	);
	public static final Block CHARRED_LOG = register(
		"charred_log",
		new PillarBlock(
			AbstractBlock.Settings.of(Material.NETHER_WOOD, MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_LOG = register(
		"stripped_charred_log",
		new PillarBlock(
			AbstractBlock.Settings.of(Material.NETHER_WOOD, MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD))
	);
	public static final Block CHARRED_WOOD = register(
		"charred_wood",
		new PillarBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_WOOD = register(
		"stripped_charred_wood",
		new PillarBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	//copy-paste of Blocks.createLeavesBlock(), but with MapColor.BLACK added.
	public static final LeavesBlock CHARRED_LEAVES = register(
		"charred_leaves",
		new LeavesBlock(
			AbstractBlock.Settings.of(Material.LEAVES, MapColor.BLACK)
			.strength(0.2F)
			.ticksRandomly()
			.sounds(BlockSoundGroup.GRASS)
			.nonOpaque()
			.allowsSpawning((state, world, pos, type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
			.suffocates((state, world, pos) -> false)
			.blockVision((state, world, pos) -> false)
		)
	);
	public static final SignBlock CHARRED_SIGN = register(
		"charred_sign",
		new SignBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.strength(1.0F)
			.sounds(BlockSoundGroup.WOOD),
			CHARRED_SIGN_TYPE
		)
	);
	public static final WallSignBlock CHARRED_WALL_SIGN = register(
		"charred_wall_sign",
		new WallSignBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.strength(1.0F)
			.sounds(BlockSoundGroup.WOOD)
			.dropsLike(CHARRED_SIGN),
			CHARRED_SIGN_TYPE
		)
	);
	public static final PressurePlateBlock CHARRED_PRESSURE_PLATE = register(
		"charred_pressure_plate",
		new PressurePlateBlock(
			PressurePlateBlock.ActivationRule.EVERYTHING,
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
				.noCollision()
				.nonOpaque()
				.strength(0.5F)
				.sounds(BlockSoundGroup.WOOD)
		) {

			@Override
			public int getTickRate() {
				return 10;
			}
		}
	);
	public static final TrapdoorBlock CHARRED_TRAPDOOR = register(
		"charred_trapdoor",
		new TrapdoorBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.strength(3.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final StairsBlock CHARRED_STAIRS = register(
		"charred_stairs",
		new StairsBlock(
			CHARRED_PLANKS.getDefaultState(),
			AbstractBlock.Settings.copy(CHARRED_PLANKS)
		)
	);
	public static final FlowerPotBlock POTTED_CHARRED_SAPLING = register(
		"potted_charred_sapling",
		newPottedPlant(CHARRED_SAPLING)
	);
	public static final AbstractButtonBlock CHARRED_BUTTON = register(
		"charred_button",
		new WoodenButtonBlock(
			AbstractBlock.Settings.of(Material.WOOD)
			.noCollision()
			.strength(0.5F)
			.sounds(BlockSoundGroup.WOOD)
		) {

			@Override
			public int getPressTicks() {
				return 10;
			}
		}
	);
	public static final SlabBlock CHARRED_SLAB = register(
		"charred_slab",
		new SlabBlock(
			AbstractBlock.Settings.copy(CHARRED_PLANKS)
		)
	);
	public static final Block CHARRED_FENCE = register(
		"charred_fence",
		new FenceBlock(
			AbstractBlock.Settings.copy(CHARRED_PLANKS)
		)
	);
	public static final FenceGateBlock CHARRED_FENCE_GATE = register(
		"charred_fence_gate",
		new FenceGateBlock(
			AbstractBlock.Settings.copy(CHARRED_PLANKS)
		)
	);
	public static final Block CHARRED_DOOR = register(
		"charred_door",
		new DoorBlock(
			AbstractBlock.Settings.of(Material.WOOD, MapColor.BLACK)
			.strength(3.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final HiddenLavaBlock HIDDEN_LAVA = register(
		"hidden_lava",
		new HiddenLavaBlock(
			AbstractBlock.Settings.of(Material.LAVA)
			.dropsNothing()
		)
	);
	public static final Block ROUGH_QUARTZ = register(
		"rough_quartz",
		new AmethystBlock(
			AbstractBlock.Settings.of(Material.AMETHYST, MapColor.OFF_WHITE)
			.strength(1.5F)
			.sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.requiresTool()
		)
	);
	public static final Block BUDDING_QUARTZ = register(
		"budding_quartz",
		new BuddingQuartzBlock(
			AbstractBlock.Settings.of(Material.AMETHYST, MapColor.OFF_WHITE)
			.ticksRandomly()
			.strength(1.5F)
			.sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.requiresTool()
		)
	);
	public static final Block QUARTZ_CLUSTER = register(
		"quartz_cluster",
		new AmethystClusterBlock(7, 3,
			AbstractBlock.Settings.of(Material.AMETHYST, MapColor.OFF_WHITE)
			.nonOpaque()
			.ticksRandomly()
			.sounds(BlockSoundGroup.AMETHYST_CLUSTER)
			.strength(1.5F)
		)
	);
	public static final Block LARGE_QUARTZ_BUD = register(
		"large_quartz_bud",
		new AmethystClusterBlock(5, 3,
			AbstractBlock.Settings.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.MEDIUM_AMETHYST_BUD)
		)
	);
	public static final Block MEDIUM_QUARTZ_BUD = register(
		"medium_quartz_bud",
		new AmethystClusterBlock(4, 3,
			AbstractBlock.Settings.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.LARGE_AMETHYST_BUD)
		)
	);
	public static final Block SMALL_QUARTZ_BUD = register(
		"small_quartz_bud",
		new AmethystClusterBlock(3, 4,
			AbstractBlock.Settings.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.SMALL_AMETHYST_BUD)
		)
	);

	static { BigGlobeMod.LOGGER.debug("Done registering blocks."); }

	//////////////////////////////// end of blocks ////////////////////////////////

	public static FlowerPotBlock newPottedPlant(Block plant) {
		int lightLevel = plant.getDefaultState().getLuminance();
		return new FlowerPotBlock(
			plant,
			AbstractBlock.Settings
			.of(Material.DECORATION)
			.breakInstantly()
			.nonOpaque()
			.luminance(state -> lightLevel)
		);
	}

	public static <B extends Block> B register(String name, B block) {
		return Registry.register(Registry.BLOCK, BigGlobeMod.modID(name), block);
	}

	public static void init() {
		TillableBlockRegistry.register(OVERGROWN_PODZOL, HoeItem::canTillFarmland, Blocks.FARMLAND.getDefaultState());
		StrippableBlockRegistry.register(CHARRED_LOG, STRIPPED_CHARRED_LOG);
		StrippableBlockRegistry.register(CHARRED_WOOD, STRIPPED_CHARRED_WOOD);
		LandPathNodeTypesRegistry.register(BLAZING_BLOSSOM, PathNodeType.DAMAGE_FIRE, PathNodeType.DANGER_FIRE);
		LandPathNodeTypesRegistry.register(SOUl_MAGMA, PathNodeType.DAMAGE_FIRE, PathNodeType.DANGER_FIRE);
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_SIGN);
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_WALL_SIGN);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		BlockRenderLayerMap.INSTANCE.putBlocks(
			RenderLayer.getCutoutMipped(),
			OVERGROWN_PODZOL
		);
		BlockRenderLayerMap.INSTANCE.putBlocks(
			RenderLayer.getCutout(),
			ROSE,
			POTTED_ROSE,
			SHORT_GRASS,
			MUSHROOM_SPORES,
			WART_WEED,
			CHARRED_GRASS,
			BLAZING_BLOSSOM,
			GLOWING_GOLDENROD,
			POTTED_BLAZING_BLOSSOM,
			POTTED_GLOWING_GOLDENROD,
			CHARRED_SAPLING,
			POTTED_CHARRED_SAPLING,
			CHARRED_DOOR,
			SMALL_QUARTZ_BUD,
			MEDIUM_QUARTZ_BUD,
			LARGE_QUARTZ_BUD,
			QUARTZ_CLUSTER
		);

		ColorProviderRegistry.BLOCK.register(
			(state, world, pos, tintIndex) -> (
				world != null && pos != null
				? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getColor(0.5D, 1.0D)
			),
			OVERGROWN_PODZOL,
			SHORT_GRASS
		);
	}
}