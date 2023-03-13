package builderb0y.bigglobe.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.registry.TillableBlockRegistry;

import net.minecraft.block.*;
import net.minecraft.block.AbstractBlock.OffsetType;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.HoeItem;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;

public class BigGlobeBlocks {

	static { BigGlobeMod.LOGGER.debug("Registering blocks..."); }

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

	static { BigGlobeMod.LOGGER.debug("Done registering blocks."); }

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
			MUSHROOM_SPORES
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