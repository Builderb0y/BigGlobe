package builderb0y.bigglobe.blocks;

import java.util.EnumMap;
import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.fabricmc.fabric.api.registry.LandPathNodeTypesRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockSetType.PressurePlateSensitivity;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.brewing.BigGlobeBrewing;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.mixinInterfaces.MutableBlockEntityType;
import builderb0y.bigglobe.mixins.Items_PlaceableFlint;
import builderb0y.bigglobe.mixins.Items_PlaceableSticks;

public class BigGlobeBlocks {

	static {
		BigGlobeMod.LOGGER.debug("Registering blocks...");
	}

	public static final BlockSetType CHARRED_BLOCK_SET_TYPE = new BlockSetTypeBuilder().pressurePlateActivationRule(PressurePlateSensitivity.EVERYTHING).register(BigGlobeMod.modID("charred"));
	public static final WoodType CHARRED_WOOD_TYPE = new WoodTypeBuilder().register(BigGlobeMod.modID("charred"), CHARRED_BLOCK_SET_TYPE);

	public static final OvergrownSandBlock OVERGROWN_SAND = register(
		"overgrown_sand",
		new OvergrownSandBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.SAND)
				.setId(key("overgrown_sand"))
				.randomTicks()
		)
	);
	public static final SnowyDirtBlock OVERGROWN_PODZOL = register(
		"overgrown_podzol",
		new SnowyDirtBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("overgrown_podzol"))
				.mapColor(MapColor.PLANT)
				.strength(0.5F)
				.sound(SoundType.GRAVEL)
		)
	);
	public static final FlowerBlock ROSE = register(
		"rose",
		new FlowerBlock(
			MobEffects.LUCK,
			5,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("rose"))
				.mapColor(MapColor.COLOR_RED)
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_ROSE = register(
		"potted_rose",
		newPottedPlant(ROSE, "potted_rose")
	);
	public static final ShortGrassBlock SHORT_GRASS = register(
		"short_grass",
		new ShortGrassBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.SHORT_GRASS)
				.setId(key("short_grass"))
				.offsetType(OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
				.replaceable()
		)
	);
	public static final MushroomSporesBlock MUSHROOM_SPORES = register(
		"mushroom_spores",
		new MushroomSporesBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("mushroom_spores"))
				.mapColor(MapColor.COLOR_PURPLE)
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
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
			BlockBehaviour
				.Properties
				.of()
				.setId(ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID("stick")))
				.mapColor(MapColor.COLOR_BROWN)
				.instabreak()
				.noCollision()
				.offsetType(OffsetType.XZ)
				.sound(SoundType.WOOD)
				.pushReaction(PushReaction.DESTROY)
		),
			FLINT = new FlintBlock(
				BlockBehaviour
					.Properties
					.of()
					.setId(ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID("flint")))
					.mapColor(MapColor.METAL)
					.instabreak()
					.noCollision()
					.offsetType(OffsetType.XZ)
					.sound(SoundType.STONE)
					.pushReaction(PushReaction.DESTROY)
			);
	}

	public static final RockBlock ROCK = register(
		"rock",
		new RockBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("rock"))
				.mapColor(MapColor.METAL)
				.instabreak()
				.noCollision()
				.offsetType(OffsetType.XZ)
				.sound(SoundType.STONE)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final SpelunkingRopeBlock SPELUNKING_ROPE = register(
		"spelunking_rope",
		new SpelunkingRopeBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("spelunking_rope"))
				.mapColor(MapColor.WOOD)
				.strength(0.8f)
				.sound(SoundType.WOOL)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final RopeAnchorBlock ROPE_ANCHOR = register(
		"rope_anchor",
		new RopeAnchorBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("rope_anchor"))
				.mapColor(MapColor.METAL)
				.requiresCorrectToolForDrops()
				.strength(5.0F)
				.sound(SoundType.DEEPSLATE_BRICKS)
				.pushReaction(PushReaction.BLOCK)
		)
	);
	public static final Block CRYSTALLINE_PRISMARINE = register(
		"crystalline_prismarine",
		new Block(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.PRISMARINE)
				.setId(key("crystalline_prismarine"))
				.lightLevel((BlockState state) -> 4)
		)
	);
	public static final Block SLATED_PRISMARINE = register(
		"slated_prismarine",
		new Block(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.DARK_PRISMARINE)
				.setId(key("slated_prismarine"))
		)
	);
	public static final SlabBlock SLATED_PRISMARINE_SLAB = register(
		"slated_prismarine_slab",
		new SlabBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(SLATED_PRISMARINE)
				.setId(key("slated_prismarine_slab"))
		)
	);
	public static final StairBlock SLATED_PRISMARINE_STAIRS = register(
		"slated_prismarine_stairs",
		new StairBlock(
			SLATED_PRISMARINE.defaultBlockState(),
			BlockBehaviour
				.Properties
				.ofFullCopy(SLATED_PRISMARINE)
				.setId(key("slated_prismarine_stairs"))
		)
	);
	public static final EnumMap<CloudColor, CloudBlock> CLOUDS = new EnumMap<>(CloudColor.class);

	static {
		for (CloudColor color : CloudColor.VALUES) {
			CLOUDS.put(
				color, register(
					color.normalName,
					new CloudBlock(
						BlockBehaviour
							.Properties
							.of()
							.setId(key(color.normalName))
							.mapColor(MapColor.SNOW)
							.strength(0.2F)
							.sound(SoundType.WOOL)
							.lightLevel(
								color == CloudColor.BLANK
									? (BlockState state) -> 0
									: (BlockState state) -> 5
							)
							.isValidSpawn(Blocks::never),
						color,
						false
					)
				)
			);
		}
	}

	public static final RiverWaterBlock RIVER_WATER = register(
		"river_water",
		new RiverWaterBlock(
			Fluids.WATER.builtInRegistryHolder(),
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.WATER)
				.setId(key("river_water"))
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
					BlockBehaviour
						.Properties
						.of()
						.setId(key("molten_rock_" + ((char)(heat + '0'))))
						.mapColor(heat > 4 ? MapColor.COLOR_ORANGE : MapColor.STONE)
						.requiresCorrectToolForDrops()
						.lightLevel((BlockState state) -> lightLevel)
						.strength(1.5F - heat / 10.0F, 6.0F - heat * (4.0F / 10.0F))
						.isValidSpawn((BlockState state, BlockGetter world, BlockPos pos, EntityType<?> entityType) -> entityType.fireImmune()),
					heat
				)
			);
		}
	}

	public static final AutomataBlock ANCIENT_AUTOMATA = register(
		"ancient_automata",
		new AutomataBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("ancient_automata"))
				.mapColor(MapColor.COLOR_BLACK)
				.requiresCorrectToolForDrops()
				.strength(1.5F, 6.0F),
			true
		)
	);
	public static final AutomataBlock AUTOMATA = register(
		"automata",
		new AutomataBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("automata"))
				.mapColor(MapColor.COLOR_BLACK)
				.requiresCorrectToolForDrops()
				.strength(1.5F, 6.0F),
			false
		)
	);
	public static final FlowerBedBlock RED_WILDFLOWERS = register(
		"red_wildflowers",
		new FlowerBedBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("red_wildflowers"))
				.mapColor(MapColor.PLANT)
				.noCollision()
				.sound(SoundType.PINK_PETALS)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerBedBlock BLUEBONNETS = register(
		"bluebonnets",
		new FlowerBedBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("bluebonnets"))
				.mapColor(MapColor.PLANT)
				.noCollision()
				.sound(SoundType.PINK_PETALS)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerBedBlock VIOLETS = register(
		"violets",
		new FlowerBedBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("violets"))
				.mapColor(MapColor.PLANT)
				.noCollision()
				.sound(SoundType.PINK_PETALS)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final DelayedGenerationBlock DELAYED_GENERATION = register(
		"delayed_generation",
		new DelayedGenerationBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("delayed_generation"))
				.mapColor(MapColor.NONE)
				.instabreak()
				.noCollision()
				.noOcclusion()
				.noLootTable()
				.pushReaction(PushReaction.BLOCK)
		)
	);

	/// ///////////////////////////// nether ////////////////////////////////

	public static final AshenNetherrackBlock ASHEN_NETHERRACK = register(
		"ashen_netherrack",
		new AshenNetherrackBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("ashen_netherrack"))
				.mapColor(MapColor.COLOR_BLACK)
				.requiresCorrectToolForDrops()
				.strength(0.4F)
				.sound(SoundType.NETHERRACK)
		)
	);
	public static final Block SULFUR_ORE = register(
		"sulfur_ore",
		new DropExperienceBlock(
			UniformInt.of(0, 2),
			BlockBehaviour
				.Properties
				.of()
				.setId(key("sulfur_ore"))
				.mapColor(MapColor.NETHER)
				.strength(3.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.NETHER_ORE)
		)
	);
	public static final Block SULFUR_BLOCK = register(
		"sulfur_block",
		new Block(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("sulfur_block"))
				.mapColor(MapColor.COLOR_YELLOW)
				.strength(5.0F, 6.0F)
				.requiresCorrectToolForDrops()
		)
	);
	public static final NetherGrassBlock WART_WEED = register(
		"wart_weed",
		new NetherGrassBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("wart_weed"))
				.mapColor(MapColor.COLOR_RED)
				.noOcclusion()
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
				.replaceable()
		)
	);
	public static final NetherGrassBlock CHARRED_GRASS = register(
		"charred_grass",
		new NetherGrassBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_grass"))
				.mapColor(MapColor.COLOR_BLACK)
				.noOcclusion()
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.pushReaction(PushReaction.DESTROY)
				.replaceable()
		)
	);
	public static final BlazingBlossomBlock BLAZING_BLOSSOM = register(
		"blazing_blossom",
		new BlazingBlossomBlock(
			MobEffects.FIRE_RESISTANCE,
			8,
			BlazingBlossomBlock.particleEntry(ParticleTypes.FLAME),
			BlockBehaviour
				.Properties
				.of()
				.setId(key("blazing_blossom"))
				.mapColor(MapColor.TERRACOTTA_ORANGE)
				.instabreak()
				.noOcclusion()
				.noCollision()
				.sound(SoundType.GRASS)
				.lightLevel((BlockState state) -> 7)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final BlazingBlossomBlock SOUL_SILVERPETAL = register(
		"soul_silverpetal",
		new BlazingBlossomBlock(
			BigGlobeBrewing.SOUL_SIPHON,
			8,
			BlazingBlossomBlock.particleEntry(ParticleTypes.SOUL_FIRE_FLAME),
			BlockBehaviour
				.Properties
				.of()
				.setId(key("soul_silverpetal"))
				.mapColor(MapColor.DIAMOND)
				.instabreak()
				.noOcclusion()
				.noCollision()
				.sound(SoundType.GRASS)
				.lightLevel((BlockState state) -> 5)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final NetherFlowerBlock GLOWING_GOLDENROD = register(
		"glowing_goldenrod",
		new NetherFlowerBlock(
			MobEffects.GLOWING,
			8,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("glowing_goldenrod"))
				.mapColor(MapColor.SAND)
				.instabreak()
				.noOcclusion()
				.noCollision()
				.sound(SoundType.GRASS)
				.lightLevel((BlockState state) -> 11)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_BLAZING_BLOSSOM = register(
		"potted_blazing_blossom",
		newPottedPlant(BLAZING_BLOSSOM, "potted_blazing_blossom")
	);
	public static final FlowerPotBlock POTTED_GLOWING_GOLDENROD = register(
		"potted_glowing_goldenrod",
		newPottedPlant(GLOWING_GOLDENROD, "potted_glowing_goldenrod")
	);
	public static final SoulLavaBlock SOUL_LAVA = register(
		"soul_lava",
		new SoulLavaBlock(
			BigGlobeFluids.SOUL_LAVA.builtInRegistryHolder(),
			BlockBehaviour
				.Properties
				.of()
				.setId(key("soul_lava"))
				.mapColor(MapColor.DIAMOND)
				.noCollision()
				.randomTicks()
				.strength(100.0F)
				.lightLevel((BlockState state) -> 15)
				.noLootTable()
				.pushReaction(PushReaction.DESTROY)
				.replaceable()
		)
	);
	public static final MagmaBlock SOUl_MAGMA = register(
		"soul_magma",
		new MagmaBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.MAGMA_BLOCK)
				.setId(key("soul_magma"))
				.mapColor(MapColor.LAPIS)
				.isValidSpawn((BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) -> type.fireImmune()) //not copied by copy().
		)
	);
	public static final SoulCauldronBlock SOUL_CAULDRON = register(
		"soul_cauldron",
		new SoulCauldronBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(Blocks.LAVA_CAULDRON)
				.setId(key("soul_cauldron"))
		)
	);
	public static final Block CHARRED_PLANKS = register(
		"charred_planks",
		new Block(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_planks"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F, 3.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final SaplingBlock CHARRED_SAPLING = register(
		"charred_sapling",
		new CharredSaplingBlock(
			new TreeGrower(
				"bigglobe:charred",
				Optional.empty(),
				Optional.of(
					ResourceKey.create(
						Registries.CONFIGURED_FEATURE,
						BigGlobeMod.modID("charred_tree_vanilla")
					)
				),
				Optional.empty()
			),
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_sapling"))
				.mapColor(MapColor.COLOR_BLACK)
				.noCollision()
				.noOcclusion()
				.randomTicks()
				.instabreak()
				.sound(SoundType.GRASS)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block CHARRED_LOG = register(
		"charred_log",
		new RotatedPillarBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_log"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_LOG = register(
		"stripped_charred_log",
		new RotatedPillarBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("stripped_charred_log"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F)
				.sound(SoundType.WOOD))
	);
	public static final Block CHARRED_WOOD = register(
		"charred_wood",
		new RotatedPillarBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_wood"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final Block STRIPPED_CHARRED_WOOD = register(
		"stripped_charred_wood",
		new RotatedPillarBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("stripped_charred_wood"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(2.0F)
				.sound(SoundType.WOOD)
		)
	);
	//copy-paste of Blocks.createLeavesBlock(), but with MapColor.BLACK added.
	public static final LeavesBlock CHARRED_LEAVES = register(
		"charred_leaves",
		new UntintedParticleLeavesBlock(
			0.02F,
			ParticleTypes.ASH,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_leaves"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(0.2F)
				.randomTicks()
				.sound(SoundType.GRASS)
				.noOcclusion()
				.isValidSpawn((BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) -> type == EntityType.OCELOT || type == EntityType.PARROT)
				.isSuffocating((BlockState state, BlockGetter world, BlockPos pos) -> false)
				.isViewBlocking((BlockState state, BlockGetter world, BlockPos pos) -> false)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final StandingSignBlock CHARRED_SIGN = register(
		"charred_sign",
		new StandingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_sign"))
				.mapColor(MapColor.COLOR_BLACK)
				.noCollision()
				.noOcclusion()
				.strength(1.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final WallSignBlock CHARRED_WALL_SIGN = register(
		"charred_wall_sign",
		new WallSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_wall_sign"))
				.mapColor(MapColor.COLOR_BLACK)
				.noCollision()
				.noOcclusion()
				.strength(1.0F)
				.sound(SoundType.WOOD)
				.overrideLootTable(CHARRED_SIGN.getLootTable())
		)
	);
	public static final CeilingHangingSignBlock CHARRED_HANGING_SIGN = register(
		"charred_hanging_sign",
		new CeilingHangingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_hanging_sign"))
				.mapColor(MapColor.COLOR_BLACK)
				.forceSolidOn()
				.instrument(NoteBlockInstrument.BASS)
				.noCollision()
				.strength(1.0F)
				.ignitedByLava()
		)
	);
	public static final WallHangingSignBlock CHARRED_WALL_HANGING_SIGN = register(
		"charred_wall_hanging_sign",
		new WallHangingSignBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_wall_hanging_sign"))
				.mapColor(MapColor.COLOR_BLACK)
				.forceSolidOn()
				.instrument(NoteBlockInstrument.BASS)
				.noCollision()
				.strength(1.0F)
				.ignitedByLava()
				.overrideLootTable(CHARRED_HANGING_SIGN.getLootTable())
		)
	);
	public static final PressurePlateBlock CHARRED_PRESSURE_PLATE = register(
		"charred_pressure_plate",
		new CharredPressurePlateBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_pressure_plate"))
				.mapColor(MapColor.COLOR_BLACK)
				.noCollision()
				.noOcclusion()
				.strength(0.5F)
				.sound(SoundType.WOOD)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final TrapDoorBlock CHARRED_TRAPDOOR = register(
		"charred_trapdoor",
		new TrapDoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_trapdoor"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(3.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final StairBlock CHARRED_STAIRS = register(
		"charred_stairs",
		new StairBlock(
			CHARRED_PLANKS.defaultBlockState(),
			BlockBehaviour
				.Properties
				.ofFullCopy(CHARRED_PLANKS)
				.setId(key("charred_stairs"))
		)
	);
	public static final FlowerPotBlock POTTED_CHARRED_SAPLING = register(
		"potted_charred_sapling",
		newPottedPlant(CHARRED_SAPLING, "potted_charred_sapling")
	);
	public static final ButtonBlock CHARRED_BUTTON = register(
		"charred_button",
		new ButtonBlock(
			CHARRED_BLOCK_SET_TYPE,
			10,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_button"))
				.mapColor(MapColor.COLOR_BLACK)
				.noCollision()
				.strength(0.5F)
				.sound(SoundType.WOOD)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final SlabBlock CHARRED_SLAB = register(
		"charred_slab",
		new SlabBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(CHARRED_PLANKS)
				.setId(key("charred_slab"))
		)
	);
	public static final FenceBlock CHARRED_FENCE = register(
		"charred_fence",
		new FenceBlock(
			BlockBehaviour
				.Properties
				.ofFullCopy(CHARRED_PLANKS)
				.setId(key("charred_fence"))
		)
	);
	public static final FenceGateBlock CHARRED_FENCE_GATE = register(
		"charred_fence_gate",
		new FenceGateBlock(
			CHARRED_WOOD_TYPE,
			BlockBehaviour
				.Properties
				.ofFullCopy(CHARRED_PLANKS)
				.setId(key("charred_fence_gate"))
		)
	);
	public static final DoorBlock CHARRED_DOOR = register(
		"charred_door",
		new DoorBlock(
			CHARRED_BLOCK_SET_TYPE,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_door"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(3.0F)
				.sound(SoundType.WOOD)
		)
	);
	public static final ShelfBlock CHARRED_SHELF = register(
		"charred_shelf",
		new ShelfBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("charred_shelf"))
				.mapColor(MapColor.COLOR_BLACK)
				.instrument(NoteBlockInstrument.BASS)
				.sound(SoundType.SHELF)
				.ignitedByLava()
				.strength(2.0F, 3.0F)
		)
	);
	public static final HiddenLavaBlock HIDDEN_LAVA = register(
		"hidden_lava",
		new HiddenLavaBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("hidden_lava"))
				.mapColor(MapColor.FIRE)
				.noLootTable()
				.pushReaction(PushReaction.DESTROY)
				.replaceable()
		)
	);
	public static final Block ROUGH_QUARTZ = register(
		"rough_quartz",
		new AmethystBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("rough_quartz"))
				.mapColor(MapColor.QUARTZ)
				.strength(1.5F)
				.sound(SoundType.AMETHYST)
				.requiresCorrectToolForDrops()
		)
	);
	public static final Block BUDDING_QUARTZ = register(
		"budding_quartz",
		new BuddingQuartzBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("budding_quartz"))
				.mapColor(MapColor.QUARTZ)
				.randomTicks()
				.strength(1.5F)
				.sound(SoundType.AMETHYST)
				.requiresCorrectToolForDrops()
		)
	);
	public static final Block QUARTZ_CLUSTER = register(
		"quartz_cluster",
		new AmethystClusterBlock(
			7, 3,
			BlockBehaviour
				.Properties
				.of()
				.setId(key("quartz_cluster"))
				.mapColor(MapColor.QUARTZ)
				.noOcclusion()
				.randomTicks()
				.sound(SoundType.AMETHYST_CLUSTER)
				.strength(1.5F)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block LARGE_QUARTZ_BUD = register(
		"large_quartz_bud",
		new AmethystClusterBlock(
			5, 3,
			BlockBehaviour
				.Properties
				.ofFullCopy(QUARTZ_CLUSTER)
				.setId(key("large_quartz_bud"))
				.sound(SoundType.MEDIUM_AMETHYST_BUD)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block MEDIUM_QUARTZ_BUD = register(
		"medium_quartz_bud",
		new AmethystClusterBlock(
			4, 3,
			BlockBehaviour
				.Properties
				.ofFullCopy(QUARTZ_CLUSTER)
				.setId(key("medium_quartz_bud"))
				.sound(SoundType.LARGE_AMETHYST_BUD)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block SMALL_QUARTZ_BUD = register(
		"small_quartz_bud",
		new AmethystClusterBlock(
			3, 4,
			BlockBehaviour
				.Properties
				.ofFullCopy(QUARTZ_CLUSTER)
				.setId(key("small_quartz_bud"))
				.sound(SoundType.SMALL_AMETHYST_BUD)
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block PALE_NETHERRACK = register(
		"pale_netherrack",
		new Block(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("pale_netherrack"))
				.mapColor(MapColor.COLOR_LIGHT_GRAY)
				.requiresCorrectToolForDrops()
				.strength(0.4F)
				.sound(SoundType.NETHERRACK)
		)
	);

	/// ///////////////////////////// end ////////////////////////////////

	public static final Block CHORUS_NYLIUM = register(
		"chorus_nylium",
		new ChorusNyliumBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("chorus_nylium"))
				.mapColor(MapColor.COLOR_PURPLE)
				.sound(SoundType.STONE)
				.strength(3.0F, 9.0F)
				.requiresCorrectToolForDrops()
		)
	);
	public static final Block OVERGROWN_END_STONE = register(
		"overgrown_end_stone",
		new OvergrownEndStoneBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("overgrown_end_stone"))
				.mapColor(MapColor.ICE)
				.sound(SoundType.STONE)
				.strength(3.0F, 9.0F)
				.requiresCorrectToolForDrops()
		)
	);
	public static final DoublePlantBlock TALL_CHORUS_SPORES = register(
		"tall_chorus_spores",
		new TallChorusSporeBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("tall_chorus_spores"))
				.mapColor(MapColor.COLOR_PURPLE)
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.noCollision()
				.noOcclusion()
				.instabreak()
				.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final ChorusSporeBlock MEDIUM_CHORUS_SPORES = register(
		"medium_chorus_spores",
		new MediumChorusSporeBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("medium_chorus_spores"))
				.mapColor(MapColor.COLOR_PURPLE)
				.replaceable()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.noCollision()
				.noOcclusion()
				.instabreak()
				.pushReaction(PushReaction.DESTROY),
			TALL_CHORUS_SPORES.builtInRegistryHolder()
		)
	);
	public static final ChorusSporeBlock SHORT_CHORUS_SPORES = register(
		"short_chorus_spores",
		new ShortChorusSporeBlock(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("short_chorus_spores"))
				.mapColor(MapColor.COLOR_PURPLE)
				.replaceable()
				.sound(SoundType.GRASS)
				.offsetType(OffsetType.XZ)
				.noCollision()
				.noOcclusion()
				.instabreak()
				.pushReaction(PushReaction.DESTROY),
			MEDIUM_CHORUS_SPORES.builtInRegistryHolder()
		)
	);
	public static final EnumMap<CloudColor, CloudBlock> VOID_CLOUDS = new EnumMap<>(CloudColor.class);

	static {
		for (CloudColor color : CloudColor.VALUES) {
			VOID_CLOUDS.put(
				color, register(
					color.voidName,
					new CloudBlock(
						BlockBehaviour
							.Properties
							.of()
							.setId(key(color.voidName))
							.mapColor(MapColor.COLOR_BLACK)
							.strength(0.2F)
							.sound(SoundType.WOOL)
							.lightLevel(
								color == CloudColor.BLANK
									? (BlockState state) -> 0
									: (BlockState state) -> 5
							)
							.isValidSpawn(Blocks::never),
						color,
						true
					)
				)
			);
		}
	}

	public static final Block VOIDMETAL_BLOCK = register(
		"voidmetal_block",
		new Block(
			BlockBehaviour
				.Properties
				.of()
				.setId(key("voidmetal_block"))
				.mapColor(MapColor.COLOR_BLACK)
				.strength(5.0F, 6.0F)
				.requiresCorrectToolForDrops()
				.sound(SoundType.METAL)
		)
	);

	//////////////////////////////// end of blocks ////////////////////////////////

	static {
		BigGlobeMod.LOGGER.debug("Done registering blocks.");
	}

	public static FlowerPotBlock newPottedPlant(Block plant, String key) {
		int lightLevel = plant.defaultBlockState().getLightEmission();
		return new FlowerPotBlock(
			plant,
			BlockBehaviour
				.Properties
				.of()
				.setId(key(key))
				.mapColor(plant.defaultMapColor())
				.instabreak()
				.noOcclusion()
				.lightLevel((BlockState state) -> lightLevel)
				.pushReaction(PushReaction.DESTROY)
		);
	}

	public static <B extends Block> B register(String name, B block) {
		Identifier id = BigGlobeMod.modID(name);
		if (!block.getDescriptionId().equals(Util.makeDescriptionId("block", id))) {
			throw new IllegalArgumentException("Name mismatch");
		}
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	public static ResourceKey<Block> key(String name) {
		return ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID(name));
	}

	public static void init() {
		register("stick", VanillaBlocks.STICK);
		register("flint", VanillaBlocks.FLINT);
		FlattenableBlockRegistry.register(OVERGROWN_PODZOL, Blocks.DIRT_PATH.defaultBlockState());
		TillableBlockRegistry.register(OVERGROWN_PODZOL, HoeItem::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState());
		StrippableBlockRegistry.register(CHARRED_LOG, STRIPPED_CHARRED_LOG);
		StrippableBlockRegistry.register(CHARRED_WOOD, STRIPPED_CHARRED_WOOD);
		LandPathNodeTypesRegistry.register(BLAZING_BLOSSOM, PathType.DAMAGE_FIRE, PathType.DANGER_FIRE);
		LandPathNodeTypesRegistry.register(SOUl_MAGMA, PathType.DAMAGE_FIRE, PathType.DANGER_FIRE);
		for (MoltenRockBlock block : MOLTEN_ROCKS) {
			LandPathNodeTypesRegistry.register(block, PathType.DAMAGE_FIRE, PathType.DANGER_FIRE);
		}
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_SIGN);
		((MutableBlockEntityType)(BlockEntityType.SIGN)).bigglobe_addValidBlock(CHARRED_WALL_SIGN);
		((MutableBlockEntityType)(BlockEntityType.HANGING_SIGN)).bigglobe_addValidBlock(CHARRED_HANGING_SIGN);
		((MutableBlockEntityType)(BlockEntityType.HANGING_SIGN)).bigglobe_addValidBlock(CHARRED_WALL_HANGING_SIGN);
		((MutableBlockEntityType)(BlockEntityType.SHELF)).bigglobe_addValidBlock(CHARRED_SHELF);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		BlockRenderLayerMap.putBlocks(
			ChunkSectionLayer.CUTOUT,
			OVERGROWN_PODZOL,
			ROSE,
			POTTED_ROSE,
			SHORT_GRASS,
			MUSHROOM_SPORES,
			RED_WILDFLOWERS,
			BLUEBONNETS,
			VIOLETS,
			WART_WEED,
			CHARRED_GRASS,
			BLAZING_BLOSSOM,
			SOUL_SILVERPETAL,
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
			(BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex) -> (
				world != null && pos != null
					? BiomeColors.getAverageGrassColor(world, pos)
					: GrassColor.getDefaultColor()
			),
			OVERGROWN_PODZOL,
			SHORT_GRASS
		);
		ColorProviderRegistry.BLOCK.register(
			(BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex) -> {
				return world != null && pos != null ? BiomeColors.getAverageWaterColor(world, pos) : -1;
			},
			RIVER_WATER
		);
		ColorProviderRegistry.BLOCK.register(
			(BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex) -> {
				if (tintIndex != 0) {
					return world != null && pos != null ? BiomeColors.getAverageGrassColor(world, pos) : GrassColor.getDefaultColor();
				}
				else {
					return -1;
				}
			},
			RED_WILDFLOWERS,
			BLUEBONNETS,
			VIOLETS
		);
	}
}