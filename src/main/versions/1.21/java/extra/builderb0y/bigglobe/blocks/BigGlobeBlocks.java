package builderb0y.bigglobe.blocks;

import java.util.EnumMap;
import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;

import net.minecraft.block.*;
import net.minecraft.block.AbstractBlock.OffsetType;
import net.minecraft.block.BlockSetType.ActivationRule;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.HoeItem;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.GrassColors;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;
import builderb0y.bigglobe.mixins.Items_PlaceableFlint;
import builderb0y.bigglobe.mixins.Items_PlaceableSticks;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeBlocks {

	static { BigGlobeMod.LOGGER.debug("Registering blocks..."); }

	public static final BlockSetType CHARRED_BLOCK_SET_TYPE = new BlockSetTypeBuilder().pressurePlateActivationRule(ActivationRule.EVERYTHING).register(BigGlobeMod.modID("charred"));
	public static final WoodType CHARRED_WOOD_TYPE = new WoodTypeBuilder().register(BigGlobeMod.modID("charred"), CHARRED_BLOCK_SET_TYPE);

	public static final OvergrownSandBlock OVERGROWN_SAND = register(
		"overgrown_sand",
		new OvergrownSandBlock(
			AbstractBlock
			.Settings
			.copy(Blocks.SAND)
			.ticksRandomly()
		)
	);
	public static final SnowyBlock OVERGROWN_PODZOL = register(
		"overgrown_podzol",
		new SnowyBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.DARK_GREEN)
			.strength(0.5F)
			.sounds(BlockSoundGroup.GRAVEL)
		)
	);
	public static final FlowerBlock ROSE = register(
		"rose",
		new FlowerBlock(
			StatusEffects.LUCK,
			5,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.RED)
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_ROSE = register("potted_rose", newPottedPlant(ROSE));
	public static final ShortGrassBlock SHORT_GRASS = register(
		"short_grass",
		new ShortGrassBlock(
			AbstractBlock
			.Settings
			.copy(Blocks.SHORT_GRASS)
			.offset(OffsetType.XZ)
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
		)
	);
	public static final MushroomSporesBlock MUSHROOM_SPORES = register(
		"mushroom_spores",
		new MushroomSporesBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PURPLE)
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
		)
	);
	/**
	these blocks are referenced very early during *minecraft's* initialization,
	before mods are loaded, via mixin.
	see {@link Items_PlaceableSticks} and {@link Items_PlaceableFlint}.
	bad things happen when BigGlobeBlocks registers its blocks too early.
	so instead we have a separate class to hold these blocks
	which doesn't register them on class initialization.
	registering the blocks is done in {@link #init()}.
	*/
	public static class VanillaBlocks {

		public static final SurfaceMaterialDecorationBlock
			STICK = new StickBlock(
				AbstractBlock
				.Settings
				.create()
				.mapColor(MapColor.BROWN)
				.breakInstantly()
				.noCollision()
				.offset(OffsetType.XZ)
				.sounds(BlockSoundGroup.WOOD)
				.pistonBehavior(PistonBehavior.DESTROY)
			),
			FLINT = new FlintBlock(
				AbstractBlock
				.Settings
				.create()
				.mapColor(MapColor.IRON_GRAY)
				.breakInstantly()
				.noCollision()
				.offset(OffsetType.XZ)
				.sounds(BlockSoundGroup.STONE)
				.pistonBehavior(PistonBehavior.DESTROY)
			);
	}
	public static final RockBlock ROCK = register(
		"rock",
		new RockBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.IRON_GRAY)
			.breakInstantly()
			.noCollision()
			.offset(OffsetType.XZ)
			.sounds(BlockSoundGroup.STONE)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final SpelunkingRopeBlock SPELUNKING_ROPE = register(
		"spelunking_rope",
		new SpelunkingRopeBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.OAK_TAN)
			.strength(0.8f)
			.sounds(BlockSoundGroup.WOOL)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final RopeAnchorBlock ROPE_ANCHOR = register(
		"rope_anchor",
		new RopeAnchorBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.IRON_GRAY)
			.requiresTool()
			.strength(5.0F)
			.sounds(BlockSoundGroup.DEEPSLATE_BRICKS)
			.pistonBehavior(PistonBehavior.BLOCK)
		)
	);
	public static final Block CRYSTALLINE_PRISMARINE = register(
		"crystalline_prismarine",
		new Block(
			AbstractBlock
			.Settings
			.copy(Blocks.PRISMARINE)
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
	public static final EnumMap<CloudColor, CloudBlock> CLOUDS = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			CLOUDS.put(color, register(
				color.normalName,
				new CloudBlock(
					AbstractBlock
					.Settings
					.create()
					.mapColor(MapColor.WHITE)
					.strength(0.2F)
					.sounds(BlockSoundGroup.WOOL)
					.luminance(
						color == CloudColor.BLANK
						? (BlockState state) -> 0
						: (BlockState state) -> 5
					)
					.allowsSpawning(Blocks::never),
					color,
					false
				)
			));
		}
	}
	public static final RiverWaterBlock RIVER_WATER = register(
		"river_water",
		new RiverWaterBlock(
			Fluids.WATER,
			AbstractBlock.Settings.copy(Blocks.WATER)
		)
	);
	public static final MoltenRockBlock[] MOLTEN_ROCKS = new MoltenRockBlock[8];
	static {
		for (int heat = 1; heat <= 8; heat++) {
			int lightLevel = (heat << 1) - 1;
			assert lightLevel <= 15;
			MOLTEN_ROCKS[heat - 1] = register(
				"molten_rock_" + ((char)(heat + '0')),
				new MoltenRockBlock(
					AbstractBlock
					.Settings
					.create()
					.mapColor(heat > 4 ? MapColor.ORANGE : MapColor.STONE_GRAY)
					.requiresTool()
					.luminance((BlockState state) -> lightLevel)
					.strength(0.6F)
					.allowsSpawning((BlockState state, BlockView world, BlockPos pos, EntityType<?> entityType) -> entityType.isFireImmune()),
					heat
				)
			);
		}
	}
	public static final DelayedGenerationBlock DELAYED_GENERATION = register(
		"delayed_generation",
		new DelayedGenerationBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.CLEAR)
			.breakInstantly()
			.noCollision()
			.nonOpaque()
			.dropsNothing()
			.pistonBehavior(PistonBehavior.BLOCK)
		)
	);

	//////////////////////////////// nether ////////////////////////////////

	public static final AshenNetherrackBlock ASHEN_NETHERRACK = register(
		"ashen_netherrack",
		new AshenNetherrackBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.requiresTool()
			.strength(0.4F)
			.sounds(BlockSoundGroup.NETHERRACK)
		)
	);
	public static final Block SULFUR_ORE = register(
		"sulfur_ore",
		new ExperienceDroppingBlock(
			UniformIntProvider.create(0, 2),
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.DARK_RED)
			.strength(3.0F)
			.requiresTool()
		)
	);
	public static final Block SULFUR_BLOCK = register(
		"sulfur_block",
		new Block(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.YELLOW)
			.strength(5.0F, 6.0F)
			.requiresTool()
		)
	);
	public static final NetherGrassBlock WART_WEED = register(
		"wart_weed",
		new NetherGrassBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.RED)
			.nonOpaque()
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
		)
	);
	public static final NetherGrassBlock CHARRED_GRASS = register(
		"charred_grass",
		new NetherGrassBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.nonOpaque()
			.noCollision()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
		)
	);
	public static final BlazingBlossomBlock BLAZING_BLOSSOM = register(
		"blazing_blossom",
		new BlazingBlossomBlock(
			StatusEffects.FIRE_RESISTANCE,
			8,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.TERRACOTTA_ORANGE)
			.breakInstantly()
			.nonOpaque()
			.noCollision()
			.sounds(BlockSoundGroup.GRASS)
			.luminance(state -> 7)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final NetherFlowerBlock GLOWING_GOLDENROD = register(
		"glowing_goldenrod",
		new NetherFlowerBlock(
			StatusEffects.GLOWING,
			8,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PALE_YELLOW)
			.breakInstantly()
			.nonOpaque()
			.noCollision()
			.sounds(BlockSoundGroup.GRASS)
			.luminance(state -> 11)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_BLAZING_BLOSSOM = register("potted_blazing_blossom", newPottedPlant(BLAZING_BLOSSOM));
	public static final FlowerPotBlock POTTED_GLOWING_GOLDENROD = register("potted_glowing_goldenrod", newPottedPlant(GLOWING_GOLDENROD));
	public static final SoulLavaBlock SOUL_LAVA = register(
		"soul_lava",
		new SoulLavaBlock(
			BigGlobeFluids.SOUL_LAVA,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.DIAMOND_BLUE)
			.noCollision()
			.ticksRandomly()
			.strength(100.0F)
			.luminance(state -> 15)
			.dropsNothing()
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
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
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(2.0F, 3.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final SaplingBlock CHARRED_SAPLING = register(
		"charred_sapling",
		new CharredSaplingBlock(
			new SaplingGenerator(
				"bigglobe:charred",
				Optional.empty(),
				Optional.of(RegistryKey.of(RegistryKeyVersions.configuredFeature(), BigGlobeMod.modID("charred_tree_vanilla"))),
				Optional.empty()
			),
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.ticksRandomly()
			.breakInstantly()
			.sounds(BlockSoundGroup.GRASS)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final Block CHARRED_LOG = register(
		"charred_log",
		new PillarBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_LOG = register(
		"stripped_charred_log",
		new PillarBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD))
	);
	public static final Block CHARRED_WOOD = register(
		"charred_wood",
		new PillarBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_WOOD = register(
		"stripped_charred_wood",
		new PillarBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(2.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	//copy-paste of Blocks.createLeavesBlock(), but with MapColor.BLACK added.
	public static final LeavesBlock CHARRED_LEAVES = register(
		"charred_leaves",
		new LeavesBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(0.2F)
			.ticksRandomly()
			.sounds(BlockSoundGroup.GRASS)
			.nonOpaque()
			.allowsSpawning((state, world, pos, type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
			.suffocates((state, world, pos) -> false)
			.blockVision((state, world, pos) -> false)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final SignBlock CHARRED_SIGN = register(
		"charred_sign",
		new SignBlock(
			CHARRED_WOOD_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.strength(1.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final WallSignBlock CHARRED_WALL_SIGN = register(
		"charred_wall_sign",
		new WallSignBlock(
			CHARRED_WOOD_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.strength(1.0F)
			.sounds(BlockSoundGroup.WOOD)
			.dropsLike(CHARRED_SIGN)
		)
	);
	public static final HangingSignBlock CHARRED_HANGING_SIGN = register(
		"charred_hanging_sign",
		new HangingSignBlock(
			CHARRED_WOOD_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.solid()
			.instrument(NoteBlockInstrument.BASS)
			.noCollision()
			.strength(1.0F)
			.burnable()
		)
	);
	public static final WallHangingSignBlock CHARRED_WALL_HANGING_SIGN = register(
		"charred_wall_hanging_sign",
		new WallHangingSignBlock(
			CHARRED_WOOD_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.solid()
			.instrument(NoteBlockInstrument.BASS)
			.noCollision()
			.strength(1.0F)
			.burnable()
			.dropsLike(CHARRED_HANGING_SIGN)
		)
	);
	public static final PressurePlateBlock CHARRED_PRESSURE_PLATE = register(
		"charred_pressure_plate",
		new CharredPressurePlateBlock(
			CHARRED_BLOCK_SET_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.noCollision()
			.nonOpaque()
			.strength(0.5F)
			.sounds(BlockSoundGroup.WOOD)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final TrapdoorBlock CHARRED_TRAPDOOR = register(
		"charred_trapdoor",
		new TrapdoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
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
	public static final ButtonBlock CHARRED_BUTTON = register(
		"charred_button",
		new ButtonBlock(
			CHARRED_BLOCK_SET_TYPE,
			10,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.noCollision()
			.strength(0.5F)
			.sounds(BlockSoundGroup.WOOD)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
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
			CHARRED_WOOD_TYPE,
			AbstractBlock.Settings.copy(CHARRED_PLANKS)
		)
	);
	public static final Block CHARRED_DOOR = register(
		"charred_door",
		new DoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(3.0F)
			.sounds(BlockSoundGroup.WOOD)
		)
	);
	public static final HiddenLavaBlock HIDDEN_LAVA = register(
		"hidden_lava",
		new HiddenLavaBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BRIGHT_RED)
			.dropsNothing()
			.pistonBehavior(PistonBehavior.DESTROY)
			.replaceable()
		)
	);
	public static final Block ROUGH_QUARTZ = register(
		"rough_quartz",
		new AmethystBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.OFF_WHITE)
			.strength(1.5F)
			.sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.requiresTool()
		)
	);
	public static final Block BUDDING_QUARTZ = register(
		"budding_quartz",
		new BuddingQuartzBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.OFF_WHITE)
			.ticksRandomly()
			.strength(1.5F)
			.sounds(BlockSoundGroup.AMETHYST_BLOCK)
			.requiresTool()
		)
	);
	public static final Block QUARTZ_CLUSTER = register(
		"quartz_cluster",
		new AmethystClusterBlock(7, 3,
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.OFF_WHITE)
			.nonOpaque()
			.ticksRandomly()
			.sounds(BlockSoundGroup.AMETHYST_CLUSTER)
			.strength(1.5F)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final Block LARGE_QUARTZ_BUD = register(
		"large_quartz_bud",
		new AmethystClusterBlock(5, 3,
			AbstractBlock
			.Settings
			.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.MEDIUM_AMETHYST_BUD)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final Block MEDIUM_QUARTZ_BUD = register(
		"medium_quartz_bud",
		new AmethystClusterBlock(4, 3,
			AbstractBlock
			.Settings
			.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.LARGE_AMETHYST_BUD)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final Block SMALL_QUARTZ_BUD = register(
		"small_quartz_bud",
		new AmethystClusterBlock(3, 4,
			AbstractBlock
			.Settings
			.copy(QUARTZ_CLUSTER)
			.sounds(BlockSoundGroup.SMALL_AMETHYST_BUD)
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);

	//////////////////////////////// end ////////////////////////////////

	public static final Block CHORUS_NYLIUM = register(
		"chorus_nylium",
		new ChorusNyliumBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PURPLE)
			.sounds(BlockSoundGroup.STONE)
			.strength(3.0F, 9.0F)
			.requiresTool()
		)
	);
	public static final Block OVERGROWN_END_STONE = register(
		"overgrown_end_stone",
		new OvergrownEndStoneBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PALE_PURPLE)
			.sounds(BlockSoundGroup.STONE)
			.strength(3.0F, 9.0F)
			.requiresTool()
		)
	);
	public static final TallPlantBlock TALL_CHORUS_SPORES = register(
		"tall_chorus_spores",
		new TallChorusSporeBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PURPLE)
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.noCollision()
			.nonOpaque()
			.breakInstantly()
			.pistonBehavior(PistonBehavior.DESTROY)
		)
	);
	public static final ChorusSporeBlock MEDIUM_CHORUS_SPORES = register(
		"medium_chorus_spores",
		new MediumChorusSporeBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PURPLE)
			.replaceable()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.noCollision()
			.nonOpaque()
			.breakInstantly()
			.pistonBehavior(PistonBehavior.DESTROY),
			TALL_CHORUS_SPORES
		)
	);
	public static final ChorusSporeBlock SHORT_CHORUS_SPORES = register(
		"short_chorus_spores",
		new ShortChorusSporeBlock(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.PURPLE)
			.replaceable()
			.sounds(BlockSoundGroup.GRASS)
			.offset(OffsetType.XZ)
			.noCollision()
			.nonOpaque()
			.breakInstantly()
			.pistonBehavior(PistonBehavior.DESTROY),
			MEDIUM_CHORUS_SPORES
		)
	);
	public static final EnumMap<CloudColor, CloudBlock> VOID_CLOUDS = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			VOID_CLOUDS.put(color, register(
				color.voidName,
				new CloudBlock(
					AbstractBlock
					.Settings
					.create()
					.mapColor(MapColor.BLACK)
					.strength(0.2F)
					.sounds(BlockSoundGroup.WOOL)
					.luminance(
						color == CloudColor.BLANK
						? (BlockState state) -> 0
						: (BlockState state) -> 5
					)
					.allowsSpawning(Blocks::never),
					color,
					true
				)
			));
		}
	}
	public static final Block VOIDMETAL_BLOCK = register(
		"voidmetal_block",
		new Block(
			AbstractBlock
			.Settings
			.create()
			.mapColor(MapColor.BLACK)
			.strength(5.0F, 6.0F)
			.requiresTool()
			.sounds(BlockSoundGroup.METAL)
		)
	);

	//////////////////////////////// end of blocks ////////////////////////////////

	static { BigGlobeMod.LOGGER.debug("Done registering blocks."); }

	public static FlowerPotBlock newPottedPlant(Block plant) {
		int lightLevel = plant.getDefaultState().getLuminance();
		return new FlowerPotBlock(
			plant,
			AbstractBlock
			.Settings
			.create()
			.mapColor(plant.getDefaultMapColor())
			.breakInstantly()
			.nonOpaque()
			.luminance((BlockState state) -> lightLevel)
			.pistonBehavior(PistonBehavior.DESTROY)
		);
	}

	public static <B extends Block> B register(String name, B block) {
		return Registry.register(RegistryVersions.block(), BigGlobeMod.modID(name), block);
	}

	public static void init() {
		register("stick", VanillaBlocks.STICK);
		register("flint", VanillaBlocks.FLINT);
		TillableBlockRegistry.register(OVERGROWN_PODZOL, HoeItem::canTillFarmland, Blocks.FARMLAND.getDefaultState());
		StrippableBlockRegistry.register(CHARRED_LOG, STRIPPED_CHARRED_LOG);
		StrippableBlockRegistry.register(CHARRED_WOOD, STRIPPED_CHARRED_WOOD);
		LandPathNodeTypesRegistry.register(BLAZING_BLOSSOM, PathNodeType.DAMAGE_FIRE, PathNodeType.DANGER_FIRE);
		LandPathNodeTypesRegistry.register(SOUl_MAGMA, PathNodeType.DAMAGE_FIRE, PathNodeType.DANGER_FIRE);
		for (MoltenRockBlock block : MOLTEN_ROCKS) {
			LandPathNodeTypesRegistry.register(block, PathNodeType.DAMAGE_FIRE, PathNodeType.DANGER_FIRE);
		}
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_SIGN);
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_WALL_SIGN);
		((MutableBlockEntityType)(BlockEntityType.HANGING_SIGN)).bigglobe_addValidBlock(CHARRED_HANGING_SIGN);
		((MutableBlockEntityType)(BlockEntityType.HANGING_SIGN)).bigglobe_addValidBlock(CHARRED_WALL_HANGING_SIGN);
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
			QUARTZ_CLUSTER,
			SHORT_CHORUS_SPORES,
			MEDIUM_CHORUS_SPORES,
			TALL_CHORUS_SPORES
		);

		ColorProviderRegistry.BLOCK.register(
			(BlockState state, BlockRenderView world, BlockPos pos, int tintIndex) -> (
				world != null && pos != null
				? BiomeColors.getGrassColor(world, pos)
				: GrassColors.getDefaultColor()
			),
			OVERGROWN_PODZOL,
			SHORT_GRASS
		);
		ColorProviderRegistry.BLOCK.register(
			(BlockState state, BlockRenderView world, BlockPos pos, int tintIndex) -> {
				return world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : -1;
			},
			RIVER_WATER
		);
	}
}