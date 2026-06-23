package builderb0y.bigglobe.blockdefs;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.fabricmc.fabric.api.registry.LandPathTypeRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;

import builderb0y.bigglobe.blocks.*;

public class OverworldBlocks {

	public static void init() {
		FlattenableBlockRegistry.register(OVERGROWN_PODZOL, Blocks.DIRT_PATH.defaultBlockState());
		TillableBlockRegistry.register(OVERGROWN_PODZOL, HoeItem::onlyIfAirAbove, Blocks.FARMLAND.defaultBlockState());
		for (MoltenRockBlock block : MOLTEN_ROCKS) {
			LandPathTypeRegistry.register(block, PathType.FIRE, PathType.FIRE_IN_NEIGHBOR);
		}
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		BlockColorRegistry.register(
			Collections.singletonList(BlockTintSources.grass()),
			OverworldBlocks.OVERGROWN_PODZOL,
			OverworldBlocks.SHORT_GRASS
		);
		BlockColorRegistry.register(
			Collections.singletonList(BlockTintSources.water()),
			OverworldBlocks.RIVER_WATER
		);
		BlockColorRegistry.register(
			List.of(
				BlockTintSources.constant(-1),
				BlockTintSources.grass()
			),
			OverworldBlocks.RED_WILDFLOWERS,
			OverworldBlocks.BLUEBONNETS,
			OverworldBlocks.VIOLETS
		);
	}

	public static final OvergrownSandBlock OVERGROWN_SAND = BigGlobeBlocks.register(
		"overgrown_sand",
		new OvergrownSandBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.SAND)
			.setId(BigGlobeBlocks.key("overgrown_sand"))
			.randomTicks()
		)
	);
	public static final SnowyBlock OVERGROWN_PODZOL = BigGlobeBlocks.register(
		"overgrown_podzol",
		new SnowyBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("overgrown_podzol"))
			.mapColor(MapColor.PLANT)
			.strength(0.5F)
			.sound(SoundType.GRAVEL)
		)
	);
	public static final FlowerBlock ROSE = BigGlobeBlocks.register(
		"rose",
		new FlowerBlock(
			MobEffects.LUCK,
			5,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("rose"))
			.mapColor(MapColor.COLOR_RED)
			.noCollision()
			.instabreak()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_ROSE = BigGlobeBlocks.register(
		"potted_rose",
		BigGlobeBlocks.newPottedPlant(ROSE, "potted_rose")
	);
	public static final ShortGrassBlock SHORT_GRASS = BigGlobeBlocks.register(
		"short_grass",
		new ShortGrassBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.SHORT_GRASS)
			.setId(BigGlobeBlocks.key("short_grass"))
			.offsetType(OffsetType.XZ)
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final MushroomSporesBlock MUSHROOM_SPORES = BigGlobeBlocks.register(
		"mushroom_spores",
		new MushroomSporesBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("mushroom_spores"))
			.mapColor(MapColor.COLOR_PURPLE)
			.noCollision()
			.instabreak()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final RockBlock ROCK = BigGlobeBlocks.register(
		"rock",
		new RockBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("rock"))
			.mapColor(MapColor.METAL)
			.instabreak()
			.noCollision()
			.offsetType(OffsetType.XZ)
			.sound(SoundType.STONE)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block CRYSTALLINE_PRISMARINE = BigGlobeBlocks.register(
		"crystalline_prismarine",
		new Block(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.PRISMARINE)
			.setId(BigGlobeBlocks.key("crystalline_prismarine"))
			.lightLevel((BlockState state) -> 4)
		)
	);
	public static final Block SLATED_PRISMARINE = BigGlobeBlocks.register(
		"slated_prismarine",
		new Block(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.DARK_PRISMARINE)
			.setId(BigGlobeBlocks.key("slated_prismarine"))
		)
	);
	public static final StairBlock SLATED_PRISMARINE_STAIRS = BigGlobeBlocks.register(
		"slated_prismarine_stairs",
		new StairBlock(
			SLATED_PRISMARINE.defaultBlockState(),
			BlockBehaviour
			.Properties
			.ofFullCopy(SLATED_PRISMARINE)
			.setId(BigGlobeBlocks.key("slated_prismarine_stairs"))
		)
	);
	public static final SlabBlock SLATED_PRISMARINE_SLAB = BigGlobeBlocks.register(
		"slated_prismarine_slab",
		new SlabBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(SLATED_PRISMARINE)
			.setId(BigGlobeBlocks.key("slated_prismarine_slab"))
		)
	);
	public static final EnumMap<CloudColor, CloudBlock> CLOUDS = new EnumMap<>(CloudColor.class);
	static {
		for (CloudColor color : CloudColor.VALUES) {
			CLOUDS.put(
				color,
				BigGlobeBlocks.register(
					color.normalName,
					new CloudBlock(
						BlockBehaviour
						.Properties
						.of()
						.setId(BigGlobeBlocks.key(color.normalName))
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

	public static final RiverWaterBlock RIVER_WATER = BigGlobeBlocks.register(
		"river_water",
		new RiverWaterBlock(
			Fluids.WATER.builtInRegistryHolder(),
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.WATER)
			.setId(BigGlobeBlocks.key("river_water"))
		)
	);

	public static final MoltenRockBlock[] MOLTEN_ROCKS = new MoltenRockBlock[8];

	static {
		for (int heat = 1; heat <= 8; heat++) {
			int lightLevel = (heat << 1) - 1;
			assert lightLevel <= 15;
			MOLTEN_ROCKS[heat - 1] = BigGlobeBlocks.register(
				"molten_rock_" + ((char)(heat + '0')),
				new MoltenRockBlock(
					BlockBehaviour
					.Properties
					.of()
					.setId(BigGlobeBlocks.key("molten_rock_" + ((char)(heat + '0'))))
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

	public static final AutomataBlock ANCIENT_AUTOMATA = BigGlobeBlocks.register(
		"ancient_automata",
		new NaturalAutomataBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("ancient_automata"))
			.mapColor(MapColor.COLOR_BLACK)
			.requiresCorrectToolForDrops()
			.strength(1.5F, 6.0F)
		)
	);
	public static final AutomataBlock AUTOMATA = BigGlobeBlocks.register(
		"automata",
		new ArtificialAutomataBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("automata"))
			.mapColor(MapColor.COLOR_BLACK)
			.requiresCorrectToolForDrops()
			.strength(1.5F, 6.0F)
		)
	);
	public static final FlowerBedBlock RED_WILDFLOWERS = BigGlobeBlocks.register(
		"red_wildflowers",
		new FlowerBedBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("red_wildflowers"))
			.mapColor(MapColor.PLANT)
			.noCollision()
			.sound(SoundType.PINK_PETALS)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerBedBlock BLUEBONNETS = BigGlobeBlocks.register(
		"bluebonnets",
		new FlowerBedBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("bluebonnets"))
			.mapColor(MapColor.PLANT)
			.noCollision()
			.sound(SoundType.PINK_PETALS)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerBedBlock VIOLETS = BigGlobeBlocks.register(
		"violets",
		new FlowerBedBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("violets"))
			.mapColor(MapColor.PLANT)
			.noCollision()
			.sound(SoundType.PINK_PETALS)
			.pushReaction(PushReaction.DESTROY)
		)
	);
}